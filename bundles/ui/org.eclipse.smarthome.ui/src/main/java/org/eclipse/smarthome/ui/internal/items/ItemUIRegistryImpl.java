/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.ui.internal.items;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemNotUniqueException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.RegistryHook;
import org.eclipse.smarthome.core.library.items.CallItem;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.ImageItem;
import org.eclipse.smarthome.core.library.items.LocationItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.PlayerItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationHelper;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.model.sitemap.ColorArray;
import org.eclipse.smarthome.model.sitemap.Default;
import org.eclipse.smarthome.model.sitemap.Group;
import org.eclipse.smarthome.model.sitemap.LinkableWidget;
import org.eclipse.smarthome.model.sitemap.Mapping;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapFactory;
import org.eclipse.smarthome.model.sitemap.Slider;
import org.eclipse.smarthome.model.sitemap.Switch;
import org.eclipse.smarthome.model.sitemap.VisibilityRule;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.internal.UIActivator;
import org.eclipse.smarthome.ui.items.ItemUIProvider;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a simple way to ask different item providers by a
 * single method call, i.e. the consumer does not need to iterate over all
 * registered providers as this is done inside this class.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson
 * @author Stefan Triller - Method to convert a state into something a sitemap entity can understand
 *
 */
public class ItemUIRegistryImpl implements ItemUIRegistry {

    private final Logger logger = LoggerFactory.getLogger(ItemUIRegistryImpl.class);

    /* the image location inside the installation folder */
    protected static final String IMAGE_LOCATION = "./webapps/images/";

    /* RegEx to extract and parse a function String <code>'\[(.*?)\((.*)\):(.*)\]'</code> */
    protected static final Pattern EXTRACT_TRANSFORMFUNCTION_PATTERN = Pattern.compile("\\[(.*?)\\((.*)\\):(.*)\\]");

    /* RegEx to identify format patterns. See java.util.Formatter#formatSpecifier (without the '%' at the very end). */
    protected static final String IDENTIFY_FORMAT_PATTERN_PATTERN = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z])";

    protected Set<ItemUIProvider> itemUIProviders = new HashSet<ItemUIProvider>();

    protected ItemRegistry itemRegistry;

    private final Map<Widget, Widget> defaultWidgets = Collections.synchronizedMap(new WeakHashMap<Widget, Widget>());

    public ItemUIRegistryImpl() {
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    public void addItemUIProvider(ItemUIProvider itemUIProvider) {
        itemUIProviders.add(itemUIProvider);
    }

    public void removeItemUIProvider(ItemUIProvider itemUIProvider) {
        itemUIProviders.remove(itemUIProvider);
    }

    @Override
    public String getCategory(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            String currentCategory = provider.getCategory(itemName);
            if (currentCategory != null) {
                return currentCategory;
            }
        }

        // use the category, if defined
        String category = getItemCategory(itemName);
        if (category != null) {
            return category.toLowerCase();
        }

        // do some reasonable default
        // try to get the item type from the item name
        Class<? extends Item> itemType = getItemType(itemName);
        if (itemType == null) {
            return null;
        }

        // we handle items here that have no specific widget,
        // e.g. the default widget of a rollerblind is "Switch".
        // We want to provide a dedicated default category for it
        // like "rollerblind".
        if (itemType.equals(NumberItem.class) || itemType.equals(ContactItem.class)
                || itemType.equals(RollershutterItem.class)) {
            return itemType.getSimpleName().replace("Item", "").toLowerCase();
        }
        return null;
    }

    @Override
    public String getLabel(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            String currentLabel = provider.getLabel(itemName);
            if (currentLabel != null) {
                return currentLabel;
            }
        }
        try {
            Item item = getItem(itemName);
            if (item.getLabel() != null) {
                return item.getLabel();
            }
        } catch (ItemNotFoundException e) {
        }

        return null;
    }

    @Override
    public Widget getWidget(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            Widget currentWidget = provider.getWidget(itemName);
            if (currentWidget != null) {
                return resolveDefault(currentWidget);
            }
        }
        return null;
    }

    @Override
    public Widget getDefaultWidget(Class<? extends Item> targetItemType, String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            Widget widget = provider.getDefaultWidget(targetItemType, itemName);
            if (widget != null) {
                return widget;
            }
        }

        // do some reasonable default, if no provider had an answer
        // if the itemType is not defined, try to get it from the item name
        Class<? extends Item> itemType = targetItemType;
        if (itemType == null) {
            itemType = getItemType(itemName);
        }
        if (itemType == null) {
            return null;
        }

        if (itemType.equals(SwitchItem.class)) {
            return SitemapFactory.eINSTANCE.createSwitch();
        }
        if (itemType.equals(GroupItem.class)) {
            return SitemapFactory.eINSTANCE.createGroup();
        }
        if (NumberItem.class.isAssignableFrom(itemType)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(ContactItem.class)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(DateTimeItem.class)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(RollershutterItem.class)) {
            return SitemapFactory.eINSTANCE.createSwitch();
        }
        if (itemType.equals(StringItem.class)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(LocationItem.class)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(CallItem.class)) {
            return SitemapFactory.eINSTANCE.createText();
        }
        if (itemType.equals(DimmerItem.class)) {
            Slider slider = SitemapFactory.eINSTANCE.createSlider();
            slider.setSwitchEnabled(true);
            return slider;
        }
        if (itemType.equals(ColorItem.class)) {
            return SitemapFactory.eINSTANCE.createColorpicker();
        }
        if (itemType.equals(PlayerItem.class)) {
            return createPlayerButtons();
        }
        if (itemType.equals(ImageItem.class)) {
            return SitemapFactory.eINSTANCE.createImage();
        }

        return null;
    }

    private Switch createPlayerButtons() {
        Switch playerItemSwitch = SitemapFactory.eINSTANCE.createSwitch();
        List<Mapping> mappings = playerItemSwitch.getMappings();
        Mapping commandMapping = null;
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(NextPreviousType.PREVIOUS.name());
        commandMapping.setLabel("<<");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(PlayPauseType.PAUSE.name());
        commandMapping.setLabel("||");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(PlayPauseType.PLAY.name());
        commandMapping.setLabel(">");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(NextPreviousType.NEXT.name());
        commandMapping.setLabel(">>");
        return playerItemSwitch;
    }

    @Override
    public String getLabel(Widget w) {
        String label = getLabelFromWidget(w);
        String labelMappedOption = null;

        // now insert the value, if the state is a string or decimal value and there is some formatting pattern defined
        // in the label
        // (i.e. it contains at least a %)
        String itemName = w.getItem();
        if (itemName != null) {
            State state = null;
            String formatPattern = getFormatPattern(label);
            StateDescription stateDescription = null;

            try {
                final Item item = getItem(itemName);
                // There is a known issue in the implementation of the method getStateDescription() of class Item
                // in the following case:
                // - the item provider returns as expected a state description without pattern but with for
                // example a min value because a min value is set in the item definition but no label with
                // pattern is set.
                // - the channel state description provider returns as expected a state description with a pattern
                // In this case, the result is no display of value by UIs because no pattern is set in the
                // returned StateDescription. What is expected is the display of a value using the pattern
                // provided by the channel state description provider.
                stateDescription = item.getStateDescription();
                if (formatPattern == null) {
                    if (stateDescription != null) {
                        final String pattern = stateDescription.getPattern();
                        if (pattern != null) {
                            label = label + " [" + pattern + "]";
                        }
                    }
                }

                String updatedPattern = getFormatPattern(label);
                if (updatedPattern != null) {
                    formatPattern = updatedPattern;

                    if (!formatPattern.isEmpty()) {
                        // TODO: TEE: we should find a more generic solution here! When
                        // using indexes in formatString this 'contains' will fail again
                        // and will cause an 'java.util.IllegalFormatConversionException:
                        // d != java.lang.String' later on when trying to format a String
                        // as %d (number).
                        if (label.contains("%d")) {
                            // a number is requested
                            state = item.getState();
                            if (!(state instanceof DecimalType)) {
                                state = item.getStateAs(DecimalType.class);
                            }
                        } else {
                            state = item.getState();
                        }
                    }
                }
            } catch (ItemNotFoundException e) {
                logger.error("Cannot retrieve item for widget {}", w.eClass().getInstanceTypeName());
            }

            if (formatPattern != null) {
                if (formatPattern.isEmpty()) {
                    label = label.substring(0, label.indexOf("[")).trim();
                } else {
                    if (state == null || state instanceof UnDefType) {
                        formatPattern = formatUndefined(formatPattern);
                    } else if (state instanceof Type) {
                        // if the channel contains options, we build a label with the mapped option value
                        if (stateDescription != null && stateDescription.getOptions() != null) {
                            for (StateOption option : stateDescription.getOptions()) {
                                if (option.getValue().equals(state.toString()) && option.getLabel() != null) {
                                    State stateOption = new StringType(option.getLabel());
                                    try {
                                        String formatPatternOption = stateOption.format(formatPattern);
                                        labelMappedOption = label.trim();
                                        labelMappedOption = labelMappedOption.substring(0,
                                                labelMappedOption.indexOf("[") + 1) + formatPatternOption + "]";
                                    } catch (IllegalArgumentException e) {
                                        logger.debug(
                                                "Mapping option value '{}' for item {} using format '{}' failed ({}); mapping is ignored",
                                                stateOption, itemName, formatPattern, e.getMessage());
                                        labelMappedOption = null;
                                    }
                                    break;
                                }
                            }
                        }

                        // The following exception handling has been added to work around a Java bug with formatting
                        // numbers. See http://bugs.sun.com/view_bug.do?bug_id=6476425
                        // Without this catch, the whole sitemap, or page can not be displayed!
                        // This also handles IllegalFormatConversionException, which is a subclass of IllegalArgument.
                        try {
                            formatPattern = ((Type) state).format(formatPattern);
                        } catch (IllegalArgumentException e) {
                            logger.warn("Exception while formatting value '{}' of item {} with format '{}': {}", state,
                                    itemName, formatPattern, e);
                            formatPattern = new String("Err");
                        }
                    }

                    label = label.trim();
                    label = label.substring(0, label.indexOf("[") + 1) + formatPattern + "]";
                }
            }
        }

        label = transform(label, labelMappedOption);

        return label;
    }

    private String getFormatPattern(String label) {
        String pattern = label.trim();
        int indexOpenBracket = pattern.indexOf("[");
        int indexCloseBracket = pattern.endsWith("]") ? pattern.length() - 1 : -1;

        if ((indexOpenBracket >= 0) && (indexCloseBracket > indexOpenBracket)) {
            return pattern.substring(indexOpenBracket + 1, indexCloseBracket);
        } else {
            return null;
        }
    }

    private String getLabelFromWidget(Widget w) {
        String label = null;
        if (w.getLabel() != null) {
            // if there is a label defined for the widget, use this
            label = w.getLabel();
        } else {
            String itemName = w.getItem();
            if (itemName != null) {
                // check if any item ui provider provides a label for this item
                label = getLabel(itemName);
                // if there is no item ui provider saying anything, simply use the name as a label
                if (label == null) {
                    label = itemName;
                }
            }
        }
        // use an empty string, if no label could be found
        return label != null ? label : "";
    }

    /**
     * Takes the given <code>formatPattern</code> and replaces it with a analog
     * String-based pattern to replace all value Occurrences with a dash ("-")
     *
     * @param formatPattern the original pattern which will be replaces by a
     *            String pattern.
     * @return a formatted String with dashes ("-") as value replacement
     */
    protected String formatUndefined(String formatPattern) {
        String undefinedFormatPattern = formatPattern.replaceAll(IDENTIFY_FORMAT_PATTERN_PATTERN, "%1\\$s");
        try {
            return String.format(undefinedFormatPattern, "-");
        } catch (Exception e) {
            logger.warn("Exception while formatting undefined value [sourcePattern={}, targetPattern={}, {}]",
                    formatPattern, undefinedFormatPattern, e);
            return "Err";
        }
    }

    /*
     * check if there is a status value being displayed on the right side of the
     * label (the right side is signified by being enclosed in square brackets [].
     * If so, check if the value starts with the call to a transformation service
     * (e.g. "[MAP(en.map):%s]") and execute the transformation in this case.
     * If the value does not start with the call to a transformation service,
     * we return the label with the mapped option value if provided (not null).
     */
    private String transform(String label, String labelMappedOption) {
        String ret = label;
        if (getFormatPattern(label) != null) {
            Matcher matcher = EXTRACT_TRANSFORMFUNCTION_PATTERN.matcher(label);
            if (matcher.find()) {
                String type = matcher.group(1);
                String pattern = matcher.group(2);
                String value = matcher.group(3);
                TransformationService transformation = TransformationHelper
                        .getTransformationService(UIActivator.getContext(), type);
                if (transformation != null) {
                    try {
                        ret = label.substring(0, label.indexOf("[") + 1) + transformation.transform(pattern, value)
                                + "]";
                    } catch (TransformationException e) {
                        logger.error("transformation throws exception [transformation={}, value={}]", transformation,
                                value, e);
                        ret = label.substring(0, label.indexOf("[") + 1) + value + "]";
                    }
                } else {
                    logger.warn(
                            "couldn't transform value in label because transformationService of type '{}' is unavailable",
                            type);
                    ret = label.substring(0, label.indexOf("[") + 1) + value + "]";
                }
            } else if (labelMappedOption != null) {
                ret = labelMappedOption;
            }
        }
        return ret;
    }

    @Override
    public String getCategory(Widget w) {
        String widgetTypeName = w.eClass().getInstanceTypeName()
                .substring(w.eClass().getInstanceTypeName().lastIndexOf(".") + 1);

        // the default is the widget type name, e.g. "switch"
        String category = widgetTypeName.toLowerCase();

        // if an icon is defined for the widget, use it
        if (w.getIcon() != null) {
            category = w.getIcon();
        } else {
            // otherwise check if any item ui provider provides an icon for this item
            String itemName = w.getItem();
            if (itemName != null) {
                String result = getCategory(itemName);
                if (result != null) {
                    category = result;
                }
            }
        }
        return category;
    }

    @Override
    public State getState(Widget w) {
        String itemName = w.getItem();
        if (itemName != null) {
            try {
                Item item = getItem(itemName);
                return convertState(w, item);
            } catch (ItemNotFoundException e) {
                logger.error("Cannot retrieve item '{}' for widget {}",
                        new Object[] { itemName, w.eClass().getInstanceTypeName() });
            }
        }
        return UnDefType.UNDEF;
    }

    /**
     * Converts an item state to the type the widget supports (if possible)
     *
     * @param w Widget in sitemap that shows the state
     * @param i item
     * @return the converted state or the original if conversion was not possible
     */
    private State convertState(Widget w, Item i) {
        State returnState = null;

        if (w instanceof Switch && i instanceof RollershutterItem) {
            // RollerShutter are represented as Switch in a Sitemap but need a PercentType state
            returnState = i.getStateAs(PercentType.class);
        } else if (w instanceof Slider) {
            if (i.getAcceptedDataTypes().contains(PercentType.class)) {
                returnState = i.getStateAs(PercentType.class);
            } else {
                returnState = i.getStateAs(DecimalType.class);
            }
        } else if (w instanceof Switch) {
            Switch sw = (Switch) w;
            if (sw.getMappings().size() == 0) {
                returnState = i.getStateAs(OnOffType.class);
            }
        }

        // if returnState is null, a conversion was not possible
        if (returnState == null) {
            // we return the original state to not break anything
            returnState = i.getState();
        }
        return returnState;
    }

    @Override
    public Widget getWidget(Sitemap sitemap, String id) {
        if (id.length() > 0) {
            // see if the id is an itemName and try to get the a widget for it
            Widget w = getWidget(id);
            if (w == null) {
                // try to get the default widget instead
                w = getDefaultWidget(null, id);
            }
            if (w != null) {
                w.setItem(id);
            } else {
                try {
                    int widgetID = Integer.valueOf(id.substring(0, 2));
                    if (widgetID < sitemap.getChildren().size()) {
                        w = sitemap.getChildren().get(widgetID);
                        for (int i = 2; i < id.length(); i += 2) {
                            int childWidgetID = Integer.valueOf(id.substring(i, i + 2));
                            if (childWidgetID < ((LinkableWidget) w).getChildren().size()) {
                                w = ((LinkableWidget) w).getChildren().get(childWidgetID);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // no valid number, so the requested page id does not exist
                }
            }
            return resolveDefault(w);
        }
        logger.warn("Cannot find page for id '{}'.", id);
        return null;
    }

    @Override
    public EList<Widget> getChildren(Sitemap sitemap) {
        EList<Widget> widgets = sitemap.getChildren();

        EList<Widget> result = new BasicEList<Widget>();
        for (Widget widget : widgets) {
            Widget resolvedWidget = resolveDefault(widget);
            if (resolvedWidget != null) {
                result.add(resolvedWidget);
            }
        }
        return result;
    }

    @Override
    public EList<Widget> getChildren(LinkableWidget w) {
        EList<Widget> widgets = null;
        if (w instanceof Group && w.getChildren().isEmpty()) {
            widgets = getDynamicGroupChildren((Group) w);
        } else {
            widgets = w.getChildren();
        }

        EList<Widget> result = new BasicEList<Widget>();
        for (Widget widget : widgets) {
            Widget resolvedWidget = resolveDefault(widget);
            if (resolvedWidget != null) {
                result.add(resolvedWidget);
            }
        }
        return result;
    }

    @Override
    public EObject getParent(Widget w) {
        Widget w2 = defaultWidgets.get(w);
        return (w2 == null) ? w.eContainer() : w2.eContainer();
    }

    private Widget resolveDefault(Widget widget) {
        if (!(widget instanceof Default)) {
            return widget;
        } else {
            String itemName = widget.getItem();
            if (itemName != null) {
                Item item = itemRegistry.get(itemName);
                if (item != null) {
                    Widget defaultWidget = getDefaultWidget(item.getClass(), item.getName());
                    if (defaultWidget != null) {
                        copyProperties(widget, defaultWidget);
                        defaultWidgets.put(defaultWidget, widget);
                        return defaultWidget;
                    }
                }
            }
            return null;
        }
    }

    private void copyProperties(Widget source, Widget target) {
        target.setItem(source.getItem());
        target.setIcon(source.getIcon());
        target.setLabel(source.getLabel());
        target.getVisibility().addAll(EcoreUtil.copyAll(source.getVisibility()));
        target.getLabelColor().addAll(EcoreUtil.copyAll(source.getLabelColor()));
        target.getValueColor().addAll(EcoreUtil.copyAll(source.getValueColor()));
    }

    /**
     * This method creates a list of children for a group dynamically.
     * If there are no explicit children defined in a sitemap, the children
     * can thus be created on the fly by iterating over the members of the group item.
     *
     * @param group The group widget to get children for
     * @return a list of default widgets provided for the member items
     */
    private EList<Widget> getDynamicGroupChildren(Group group) {
        EList<Widget> children = new BasicEList<Widget>();
        String itemName = group.getItem();
        try {
            if (itemName != null) {
                Item item = getItem(itemName);
                if (item instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) item;
                    for (Item member : groupItem.getMembers()) {
                        Widget widget = getDefaultWidget(member.getClass(), member.getName());
                        if (widget != null) {
                            widget.setItem(member.getName());
                            children.add(widget);
                        }
                    }
                } else {
                    logger.warn("Item '{}' is not a group.", item.getName());
                }
            } else {
                logger.warn("Group does not specify an associated item - ignoring it.");
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Group '{}' could not be found.", group.getLabel(), e);
        }
        return children;

    }

    private Class<? extends Item> getItemType(@NonNull String itemName) {
        try {
            Item item = itemRegistry.getItem(itemName);
            return item.getClass();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    @Override
    public State getItemState(String itemName) {
        try {
            Item item = itemRegistry.getItem(itemName);
            return item.getState();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    public String getItemCategory(@NonNull String itemName) {
        try {
            Item item = itemRegistry.getItem(itemName);
            return item.getCategory();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    @Override
    public Item getItem(String name) throws ItemNotFoundException {
        if (itemRegistry != null) {
            return itemRegistry.getItem(name);
        } else {
            throw new ItemNotFoundException(name);
        }
    }

    @Override
    public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException {
        if (itemRegistry != null) {
            return itemRegistry.getItemByPattern(name);
        } else {
            throw new ItemNotFoundException(name);
        }
    }

    @SuppressWarnings("null")
    @Override
    public Collection<Item> getItems() {
        if (itemRegistry != null) {
            return itemRegistry.getItems();
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("null")
    @Override
    public Collection<Item> getItemsOfType(String type) {
        if (itemRegistry != null) {
            return itemRegistry.getItemsOfType(type);
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("null")
    @Override
    public Collection<Item> getItems(String pattern) {
        if (itemRegistry != null) {
            return itemRegistry.getItems(pattern);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Item> listener) {
        if (itemRegistry != null) {
            itemRegistry.addRegistryChangeListener(listener);
        }
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Item> listener) {
        if (itemRegistry != null) {
            itemRegistry.removeRegistryChangeListener(listener);
        }
    }

    @Override
    public Collection<Item> getAll() {
        return itemRegistry.getAll();
    }

    @Override
    public Stream<Item> stream() {
        return itemRegistry.stream();
    }

    @Override
    public String getWidgetId(Widget widget) {
        Widget w2 = defaultWidgets.get(widget);
        if (w2 != null) {
            return getWidgetId(w2);
        }

        String id = "";
        Widget w = widget;
        while (w.eContainer() instanceof Widget) {
            Widget parent = (Widget) w.eContainer();
            String index = String.valueOf(((LinkableWidget) parent).getChildren().indexOf(w));
            if (index.length() == 1) {
                index = "0" + index; // make it two digits
            }
            id = index + id;
            w = parent;
        }
        if (w.eContainer() instanceof Sitemap) {
            Sitemap sitemap = (Sitemap) w.eContainer();
            String index = String.valueOf(sitemap.getChildren().indexOf(w));
            if (index.length() == 1) {
                index = "0" + index; // make it two digits
            }
            id = index + id;
        }

        // if the widget is dynamically created and not available in the sitemap,
        // use the item name as the id
        if (w.eContainer() == null) {
            String itemName = w.getItem();
            id = itemName;
        }
        return id;
    }

    private boolean matchStateToValue(State state, String value, String matchCondition) {
        // Check if the value is equal to the supplied value
        boolean matched = false;

        // Remove quotes - this occurs in some instances where multiple types
        // are defined in the xtext definitions
        String unquotedValue = value;
        if (unquotedValue.startsWith("\"") && unquotedValue.endsWith("\"")) {
            unquotedValue = unquotedValue.substring(1, unquotedValue.length() - 1);
        }

        // Convert the condition string into enum
        Condition condition = Condition.EQUAL;
        if (matchCondition != null) {
            condition = Condition.fromString(matchCondition);
        }

        if (DecimalType.class.isInstance(state)) {
            try {
                switch (condition) {
                    case EQUAL:
                        if (Double.parseDouble(state.toString()) == Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case LTE:
                        if (Double.parseDouble(state.toString()) <= Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case GTE:
                        if (Double.parseDouble(state.toString()) >= Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case GREATER:
                        if (Double.parseDouble(state.toString()) > Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case LESS:
                        if (Double.parseDouble(state.toString()) < Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case NOT:
                    case NOTEQUAL:
                        if (Double.parseDouble(state.toString()) != Double.parseDouble(unquotedValue)) {
                            matched = true;
                        }
                        break;
                }
            } catch (NumberFormatException e) {
                logger.debug("matchStateToValue: Decimal format exception: ", e);
            }
        } else if (state instanceof DateTimeType) {
            Calendar val = ((DateTimeType) state).getCalendar();
            Calendar now = Calendar.getInstance();
            long secsDif = (now.getTimeInMillis() - val.getTimeInMillis()) / 1000;

            try {
                switch (condition) {
                    case EQUAL:
                        if (secsDif == Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case LTE:
                        if (secsDif <= Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case GTE:
                        if (secsDif >= Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case GREATER:
                        if (secsDif > Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case LESS:
                        if (secsDif < Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                    case NOT:
                    case NOTEQUAL:
                        if (secsDif != Integer.parseInt(unquotedValue)) {
                            matched = true;
                        }
                        break;
                }
            } catch (NumberFormatException e) {
                logger.debug("matchStateToValue: Decimal format exception: ", e);
            }
        } else {
            // Strings only allow = and !=
            switch (condition) {
                case NOT:
                case NOTEQUAL:
                    if (!unquotedValue.equals(state.toString())) {
                        matched = true;
                    }
                    break;
                default:
                    if (unquotedValue.equals(state.toString())) {
                        matched = true;
                    }
                    break;
            }
        }

        return matched;
    }

    private String processColorDefinition(State state, List<ColorArray> colorList) {
        // Sanity check
        if (colorList == null) {
            return null;
        }
        if (colorList.size() == 0) {
            return null;
        }

        String colorString = null;

        // Check for the "arg". If it doesn't exist, assume there's just an
        // static colour
        if (colorList.size() == 1 && colorList.get(0).getState() == null) {
            colorString = colorList.get(0).getArg();
        } else {
            // Loop through all elements looking for the definition associated
            // with the supplied value
            for (ColorArray color : colorList) {
                // Use a local state variable in case it gets overridden below
                State cmpState = state;

                if (color.getState() == null) {
                    logger.error("Error parsing color");
                    continue;
                }

                // If there's an item defined here, get its state
                String itemName = color.getItem();
                if (itemName != null) {
                    // Try and find the item to test.
                    // If it's not found, return visible
                    Item item;
                    try {
                        item = itemRegistry.getItem(itemName);

                        // Get the item state
                        cmpState = item.getState();
                    } catch (ItemNotFoundException e) {
                        logger.warn("Cannot retrieve color item {} for widget", color.getItem());
                    }
                }

                // Handle the sign
                String value;
                if (color.getSign() != null) {
                    value = color.getSign() + color.getState();
                } else {
                    value = color.getState();
                }

                if (matchStateToValue(cmpState, value, color.getCondition()) == true) {
                    // We have the color for this value - break!
                    colorString = color.getArg();
                    break;
                }
            }
        }

        // Remove quotes off the colour - if they exist
        if (colorString == null) {
            return null;
        }

        if (colorString.startsWith("\"") && colorString.endsWith("\"")) {
            colorString = colorString.substring(1, colorString.length() - 1);
        }

        return colorString;
    }

    @Override
    public String getLabelColor(Widget w) {
        return processColorDefinition(getState(w), w.getLabelColor());
    }

    @Override
    public String getValueColor(Widget w) {
        return processColorDefinition(getState(w), w.getValueColor());
    }

    @Override
    public boolean getVisiblity(Widget w) {
        // Default to visible if parameters not set
        List<VisibilityRule> ruleList = w.getVisibility();
        if (ruleList == null) {
            return true;
        }
        if (ruleList.size() == 0) {
            return true;
        }

        logger.debug("Checking visiblity for widget '{}'.", w.getLabel());

        for (VisibilityRule rule : w.getVisibility()) {
            String itemName = rule.getItem();
            if (itemName == null) {
                continue;
            }
            if (rule.getState() == null) {
                continue;
            }

            // Try and find the item to test.
            // If it's not found, return visible
            Item item;
            try {
                item = itemRegistry.getItem(itemName);
            } catch (ItemNotFoundException e) {
                logger.error("Cannot retrieve visibility item {} for widget {}", rule.getItem(),
                        w.eClass().getInstanceTypeName());

                // Default to visible!
                return true;
            }

            // Get the item state
            State state = item.getState();

            // Handle the sign
            String value;
            if (rule.getSign() != null) {
                value = rule.getSign() + rule.getState();
            } else {
                value = rule.getState();
            }

            if (matchStateToValue(state, value, rule.getCondition()) == true) {
                // We have the name for this value!
                return true;
            }
        }

        logger.debug("Widget {} is not visible.", w.getLabel());

        // The state wasn't in the list, so we don't display it
        return false;
    }

    enum Condition {
        EQUAL("=="),
        GTE(">="),
        LTE("<="),
        NOTEQUAL("!="),
        GREATER(">"),
        LESS("<"),
        NOT("!");

        private String value;

        private Condition(String value) {
            this.value = value;
        }

        public static Condition fromString(String text) {
            if (text != null) {
                for (Condition c : Condition.values()) {
                    if (text.equalsIgnoreCase(c.value)) {
                        return c;
                    }
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    @SuppressWarnings("null")
    @Override
    public Collection<Item> getItemsByTag(String... tags) {
        if (itemRegistry != null) {
            return itemRegistry.getItemsByTag(tags);
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("null")
    @Override
    public Collection<Item> getItemsByTagAndType(String type, String... tags) {
        if (itemRegistry != null) {
            return itemRegistry.getItemsByTagAndType(type, tags);
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("null")
    @Override
    public <T extends GenericItem> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags) {
        if (itemRegistry != null) {
            return itemRegistry.getItemsByTag(typeFilter, tags);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Item add(Item element) {
        if (itemRegistry != null) {
            return itemRegistry.add(element);
        }
        return element;
    }

    @Override
    public Item update(Item element) {
        if (itemRegistry != null) {
            return itemRegistry.update(element);
        } else {
            return null;
        }
    }

    @Override
    public Item remove(String key) {
        if (itemRegistry != null) {
            return itemRegistry.remove(key);
        } else {
            return null;
        }
    }

    @Override
    public Item get(String key) {
        if (itemRegistry != null) {
            return itemRegistry.get(key);
        } else {
            return null;
        }
    }

    @Override
    public Item remove(String itemName, boolean recursive) {

        if (itemRegistry != null) {
            return itemRegistry.remove(itemName, recursive);
        } else {
            return null;
        }

    }

    @Override
    public void addRegistryHook(RegistryHook<Item> hook) {
        if (itemRegistry != null) {
            itemRegistry.addRegistryHook(hook);
        }
    }

    @Override
    public void removeRegistryHook(RegistryHook<Item> hook) {
        if (itemRegistry != null) {
            itemRegistry.removeRegistryHook(hook);
        }
    }

}
