/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.io.console.internal.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;

/**
 * Console command extension to get item list
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Markus Rathgeb - Create DS for command extension
 * @author Dennis Nobel - Changed service references to be injected via DS
 * @author Simon Kaufmann - Added commands to clear and remove items
 *
 */
public class ItemConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_CLEAR = "clear";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_ADDTAG = "addTag";
    private static final String SUBCMD_REMTAG = "rmTag";

    private ItemRegistry itemRegistry;

    public ItemConsoleCommandExtension() {
        super("items", "Access the item registry.");
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(SUBCMD_LIST + " [<pattern>]",
                        "lists names and types of all items (matching the pattern, if given)"),
                buildCommandUsage(SUBCMD_CLEAR, "removes all items"),
                buildCommandUsage(SUBCMD_REMOVE + " <itemName>", "removes the given item"),
                buildCommandUsage(SUBCMD_ADDTAG + " <itemName> <tag>", "adds a tag to the given item"),
                buildCommandUsage(SUBCMD_REMTAG + " <itemName> <tag>", "removes a tag from the given item") });
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    listItems(console, (args.length < 2) ? "*" : args[1]);
                    break;
                case SUBCMD_CLEAR:
                    removeItems(console, itemRegistry.getAll());
                    break;
                case SUBCMD_REMOVE:
                    if (args.length > 1) {
                        String name = args[1];
                        Item item = itemRegistry.get(name);
                        removeItems(console, Collections.singleton(item));
                    } else {
                        console.println("Specify the name of the item to remove: " + this.getCommand() + " "
                                + SUBCMD_REMOVE + " <itemName>");
                    }
                    break;
                case SUBCMD_ADDTAG:
                    if (args.length > 2) {
                        addTag(args[1], args[2], console);
                    } else {
                        console.println("Specify the name of the item and the tag: " + this.getCommand() + " "
                                + SUBCMD_ADDTAG + " <itemName> <tag>");
                    }
                    break;
                case SUBCMD_REMTAG:
                    if (args.length > 2) {
                        removeTag(args[1], args[2], console);
                    } else {
                        console.println("Specify the name of the item and the tag: " + this.getCommand() + " "
                                + SUBCMD_REMTAG + " <itemName> <tag>");
                    }
                    break;
                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            listItems(console, "*");
        }
    }

    private void addTag(String itemName, String tag, Console console) {
        Item item = itemRegistry.get(itemName);
        if (item instanceof GenericItem) {
            GenericItem gItem = (GenericItem) item;
            gItem.addTag(tag);
            Item oldItem = itemRegistry.update(gItem);
            if (oldItem == null) {
                console.println("Error: Cannot add tag " + tag + " to item " + itemName
                        + " because this item does not belong to a ManagedProvider");
            } else {
                console.println("Successfully added tag " + tag + " to item " + itemName);
            }
        } else {
            console.println(
                    "Error: Cannot add tag " + tag + " to item " + itemName + " because it is not a GenericItem");
        }
    }

    private void removeTag(String itemName, String tag, Console console) {
        Item item = itemRegistry.get(itemName);
        if (item instanceof GenericItem) {
            GenericItem gItem = (GenericItem) item;
            gItem.removeTag(tag);
            Item oldItem = itemRegistry.update(gItem);
            if (oldItem == null) {
                console.println("Error: Cannot remove tag " + tag + " from item " + itemName
                        + " because this item does not belong to a ManagedProvider");
            } else {
                console.println("Successfully removed tag " + tag + " from item " + itemName);
            }
        } else {
            console.println(
                    "Error: Cannot remove tag " + tag + " from item " + itemName + " because it is not a GenericItem");
        }
    }

    private void removeItems(Console console, Collection<Item> items) {
        int count = items.size();
        for (Item item : items) {
            itemRegistry.remove(item.getName());
        }
        console.println(count + " item(s) removed successfully.");
    }

    private void listItems(Console console, String pattern) {
        Collection<Item> items = this.itemRegistry.getItems(pattern);
        if (items.size() > 0) {
            for (Item item : items) {
                console.println(item.toString());
            }
        } else {
            console.println("No item found for this pattern.");
        }
    }

    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

}
