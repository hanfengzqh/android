/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.editors.theme.attributes.editors;

import com.android.resources.ResourceType;
import com.android.tools.idea.editors.theme.*;
import com.android.tools.idea.editors.theme.datamodels.EditedStyleItem;
import com.android.tools.idea.editors.theme.preview.AndroidThemePreviewPanel;
import com.android.tools.idea.editors.theme.ui.ResourceComponent;
import com.android.tools.idea.rendering.ResourceHelper;
import com.android.tools.swing.ui.SwatchComponent;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeFormat;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * Class that implements a {@link javax.swing.JTable} renderer and editor for color attributes.
 */
public class ColorRendererEditor extends GraphicalResourceRendererEditor {
  public ColorRendererEditor(@NotNull ThemeEditorContext context, @NotNull AndroidThemePreviewPanel previewPanel, boolean isEditor) {
    super(context, previewPanel, isEditor);
  }

  @Override
  protected void updateComponent(@NotNull ThemeEditorContext context, @NotNull ResourceComponent component, @NotNull EditedStyleItem item) {
    assert context.getResourceResolver() != null;

    final List<Color> colors = ResourceHelper.resolveMultipleColors(context.getResourceResolver(), item.getSelectedValue(), context.getProject());
    component.setSwatchIcons(SwatchComponent.colorListOf(colors));
    component.setNameText(item.getQualifiedName());
    component.setValueText(item.getValue());

    Set<String> lowContrastColors = ColorUtils.getLowContrastColors(context, item);
    if (!lowContrastColors.isEmpty()) {
      component.setWarningVisible(true);
      // Using html for the tooltip because the color names are bold
      // Formatted color names are concatenated into an error message
      StringBuilder contrastErrorMessageBuilder = new StringBuilder("<html>Not enough contrast with ");
      int i = 0;
      for (String color : lowContrastColors) {
        contrastErrorMessageBuilder.append(color);
        if (i < lowContrastColors.size() - 2) {
          contrastErrorMessageBuilder.append(", ");
        }
        else if (i == lowContrastColors.size() - 2) {
          contrastErrorMessageBuilder.append(" and ");
        }
        i++;
      }
      myComponent.setWarning(contrastErrorMessageBuilder.toString());
    }
    else {
      component.setWarningVisible(false);
    }
  }

  @NotNull
  @Override
  protected ResourceType[] getAllowedResourceTypes() {
    AttributeDefinition attrDefinition = ResolutionUtils.getAttributeDefinition(myContext.getConfiguration(), myItem.getSelectedValue());

    String attributeName = myItem.getName().toLowerCase();
    if (attributeName.contains("color") || !ThemeEditorUtils.acceptsFormat(attrDefinition, AttributeFormat.Reference)) {
      return COLORS_ONLY;
    }
    else if (attributeName.contains("drawable") || !ThemeEditorUtils.acceptsFormat(attrDefinition, AttributeFormat.Color)) {
      return DRAWABLES_ONLY;
    }
    else {
      return COLORS_AND_DRAWABLES;
    }
  }
}