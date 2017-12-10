/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.ui.classic.internal.render;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.smarthome.model.sitemap.Group;
import org.eclipse.smarthome.model.sitemap.LinkableWidget;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.classic.render.RenderException;
import org.eclipse.smarthome.ui.classic.render.WidgetRenderer;

/**
 * This is an implementation of the {@link WidgetRenderer} interface, which
 * can produce HTML code for Group widgets.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Gaël L'hopital - Added expanded behaviour handling
 *
 */
public class GroupRenderer extends AbstractWidgetRenderer {

    @Override
    public boolean canRender(Widget w) {
        return w instanceof Group;
    }

    @Override
    public EList<Widget> renderWidget(Widget w, StringBuilder sb) throws RenderException {
        String snippet;
        EList<Widget> result;

        if (((Group) w).isExpanded()) {
            snippet = getSnippet("group_expanded");
            result = itemUIRegistry.getChildren((LinkableWidget) w);
        } else {
            snippet = getSnippet("group");

            snippet = StringUtils.replace(snippet, "%id%", itemUIRegistry.getWidgetId(w));
            snippet = StringUtils.replace(snippet, "%category%", getCategory(w));
            snippet = StringUtils.replace(snippet, "%label%", getLabel(w));
            snippet = StringUtils.replace(snippet, "%state%", getState(w));
            snippet = StringUtils.replace(snippet, "%format%", getFormat());

            // Process the color tags
            snippet = processColor(w, snippet);

            result = null;
        }

        sb.append(snippet);

        return result;
    }
}
