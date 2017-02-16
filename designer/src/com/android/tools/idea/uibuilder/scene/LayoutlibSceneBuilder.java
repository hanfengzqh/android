/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.scene;

import com.android.tools.idea.configurations.ConfigurationListener;
import com.android.tools.idea.uibuilder.api.ViewGroupHandler;
import com.android.tools.idea.uibuilder.api.ViewHandler;
import com.android.tools.idea.uibuilder.model.*;
import com.android.tools.idea.uibuilder.surface.SceneView;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.android.tools.idea.configurations.ConfigurationListener.CFG_DEVICE;

/**
 * {@link SceneBuilder} that creates a Scene from an NlModel representing a layout using layoutlib.
 */
public class LayoutlibSceneBuilder extends SceneBuilder {

  private int myDpi = 0;

  public LayoutlibSceneBuilder(@NotNull NlModel model, @NotNull SceneView sceneView) {
    super(model, sceneView);
  }

  /**
   * Creates a {@link Scene} from our {@link NlModel}. This must only be called once per builder.
   * @return
   */
  @NotNull
  @Override
  public Scene build() {
    Scene scene = super.build();
    getSceneView().getSelectionModel().addListener(new SelectionChangeListener());
    NlModel model = getModel();
    ConfigurationListener listener = (flags) -> {
      if ((flags & CFG_DEVICE) != 0) {
        int newDpi = model.getConfiguration().getDensity().getDpiValue();
        if (myDpi != newDpi) {
          // Update from the model to update the dpi
          update();
        }
      }
      return true;
    };
    model.getConfiguration().addListener(listener);
    Disposer.register(model, () -> model.getConfiguration().removeListener(listener));

    List<NlComponent> components = model.getComponents();
    if (components.size() != 0) {
      NlComponent rootComponent = components.get(0).getRoot();
      scene.setAnimate(false);
      SceneComponent root = updateFromComponent(rootComponent, new HashSet<>());
      scene.setRoot(root);
      addTargets(root);
      scene.setAnimate(true);
    }
    model.addListener(new ModelChangeListener());
    // let's make sure the selection is correct
    scene.selectionChanged(getSceneView().getSelectionModel(), getSceneView().getSelectionModel().getSelection());

    return scene;
  }

  /**
   * Update the Scene with the components in the given NlModel. This method needs to be called in the dispatch thread.
   * {@link #build()} must have been invoked already.
   */
  @Override
  public void update() {
    super.update();

    List<NlComponent> components = getModel().getComponents();
    Scene scene = getScene();
    if (components.size() == 0) {
      scene.getSceneComponents().forEach(scene::removeComponent);
      scene.setRoot(null);
      return;
    }
    Set<SceneComponent> usedComponents = new HashSet<>();
    Set<SceneComponent> oldComponents = new HashSet<>(scene.getSceneComponents());

    NlComponent rootComponent = components.get(0).getRoot();

    SceneComponent root = updateFromComponent(rootComponent, usedComponents);
    oldComponents.removeAll(usedComponents);
    oldComponents.forEach(scene::removeComponent);

    SelectionModel selectionModel = getSceneView().getSelectionModel();
    scene.setRoot(root);
    if (root != null && selectionModel.isEmpty()) {
      addTargets(root);
    }
    scene.needsRebuildList();
  }

  /**
   * Creates a {@link TemporarySceneComponent} in our Scene.
   */
  @Override
  public TemporarySceneComponent createTemporaryComponent(NlComponent component) {
    return new TemporarySceneComponent(getScene(), component) {
      @Override
      void init() {
        updateFromComponent(component, this);
      }
    };
  }

  /**
   * Update (and if necessary, create) the SceneComponent paired to the given NlComponent
   *
   * @param component a given NlComponent
   * @param seenComponents Collector of components that were seen during NlComponent tree traversal.
   * @return the SceneComponent paired with the given NlComponent
   */
  private SceneComponent updateFromComponent(@NotNull NlComponent component, Set<SceneComponent> seenComponents) {
    SceneComponent sceneComponent = getScene().getSceneComponent(component);
    if (sceneComponent == null) {
      sceneComponent = new SceneComponent(getScene(), component);
    }
    seenComponents.add(sceneComponent);

    updateFromComponent(component, sceneComponent);

    for (NlComponent nlChild : component.getChildren()) {
      SceneComponent child = updateFromComponent(nlChild, seenComponents);
      if (child.getParent() != sceneComponent) {
        sceneComponent.addChild(child);
      }
    }
    return sceneComponent;
  }

  private void updateFromComponent(@NotNull NlComponent component, SceneComponent sceneComponent) {
    if (getScene().getAnimate()) {
      long time = System.currentTimeMillis();
      sceneComponent.setPositionTarget(Coordinates.pxToDp(component.getModel(), component.x),
                                       Coordinates.pxToDp(component.getModel(), component.y),
                                       time);
      sceneComponent.setSizeTarget(Coordinates.pxToDp(component.getModel(), component.w),
                                   Coordinates.pxToDp(component.getModel(), component.h),
                                   time);
    }
    else {
      sceneComponent.setPosition(Coordinates.pxToDp(component.getModel(), component.x),
                                 Coordinates.pxToDp(component.getModel(), component.y));
      sceneComponent.setSize(Coordinates.pxToDp(component.getModel(), component.w),
                             Coordinates.pxToDp(component.getModel(), component.h));
    }
  }


  /**
   * Add targets to the given component (by asking the associated
   * {@linkplain ViewGroupHandler} to do it)
   */
  private void addTargets(@NotNull SceneComponent component) {
    SceneComponent parent = component.getParent();
    if (parent != null) {
      component = parent;
    }
    else {
      component = getScene().getRoot();
    }
    ViewHandler handler = component.getNlComponent().getViewHandler();
    if (handler instanceof ViewGroupHandler) {
      ViewGroupHandler viewGroupHandler = (ViewGroupHandler)handler;
      component.setTargetProvider(viewGroupHandler, true);
      for (SceneComponent child : component.getChildren()) {
        child.setTargetProvider(viewGroupHandler, false);
      }
    }
  }

  private class ModelChangeListener implements ModelListener {
    @Override
    public void modelChanged(@NotNull NlModel model) {
      // updateFrom needs to be called in the dispatch thread
      UIUtil.invokeLaterIfNeeded(LayoutlibSceneBuilder.this::update);
    }

    @Override
    public void modelRendered(@NotNull NlModel model) {
      // updateFrom needs to be called in the dispatch thread
      UIUtil.invokeLaterIfNeeded(LayoutlibSceneBuilder.this::update);
    }

    @Override
    public void modelChangedOnLayout(@NotNull NlModel model, boolean animate) {
      boolean previous = getScene().getAnimate();
      UIUtil.invokeLaterIfNeeded(() -> {
        getScene().setAnimate(animate);
        update();
        getScene().setAnimate(previous);
      });
    }
  }

  private class SelectionChangeListener implements SelectionListener {
    @Override
    public void selectionChanged(@NotNull SelectionModel model, @NotNull List<NlComponent> selection) {
      SceneComponent root = getScene().getRoot();
      if (root != null) {
        clearChildTargets(root);
        // After a new selection, we need to figure out the context
        if (!selection.isEmpty()) {
          NlComponent primary = selection.get(0);
          SceneComponent component = getScene().getSceneComponent(primary);
          if (component != null) {
            addTargets(component);
          }
          else {
            addTargets(root);
          }
        }
        else {
          addTargets(root);
        }
      }
      getScene().needsRebuildList();
    }

    void clearChildTargets(SceneComponent component) {
      component.setTargetProvider(null, true);
      for (SceneComponent child : component.getChildren()) {
        component.setTargetProvider(null, false);
        clearChildTargets(child);
      }
    }
  }
}
