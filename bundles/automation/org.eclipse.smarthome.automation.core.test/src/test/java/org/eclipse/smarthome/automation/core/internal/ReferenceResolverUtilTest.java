package org.eclipse.smarthome.automation.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceResolverUtilTest {
    private static final String CONTEXT_PROPERTY1 = "contextProperty1";
    private static final String CONTEXT_PROPERTY2 = "contextProperty2";
    private static final String CONTEXT_PROPERTY3 = "contextProperty3";
    private static final String CONTEXT_PROPERTY4 = "contextProperty4";

    private static final Map<String, Object> context = new HashMap<String, Object>();
    private static final Map<String, Object> moduleConfiguration = new HashMap<String, Object>();
    private static final Map<String, Object> expectedModuleConfiguration = new HashMap<String, Object>();
    private static final Map<String, String> compositeChildModuleInputsReferences = new HashMap<String, String>();
    private static final Map<String, Object> expectedCompositeChildModuleContext = new HashMap<String, Object>();

    static {
        // context from where references will be taken
        context.put(CONTEXT_PROPERTY1, "value1");
        context.put(CONTEXT_PROPERTY2, "value2");
        context.put(CONTEXT_PROPERTY3, "value3");
        context.put(CONTEXT_PROPERTY4, 12345);

        // module configuration with references
        moduleConfiguration.put("simpleReference", "$" + CONTEXT_PROPERTY4);
        moduleConfiguration.put("complexReference",
                String.format("Hello ${%s} ${%s}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceWithMissing",
                String.format("Testing ${UNKNOWN}, ${%s}", CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceArray",
                String.format("[${%s}, ${%s}, staticText]", CONTEXT_PROPERTY2, CONTEXT_PROPERTY3));
        moduleConfiguration.put("complexReferenceArrayWithMissing",
                String.format("[${UNKNOWN}, ${%s}, staticText]", CONTEXT_PROPERTY3));
        moduleConfiguration.put("complexReferenceObj",
                String.format("{key1: ${%s}, key2: staticText, key3: ${%s}}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceObjWithMissing",
                String.format("{key1: ${UNKNOWN}, key2: ${%s}, key3: ${UNKNOWN2}}", CONTEXT_PROPERTY2));

        // expected resolved module configuration
        expectedModuleConfiguration.put("simpleReference", context.get(CONTEXT_PROPERTY4));
        expectedModuleConfiguration.put("complexReference",
                String.format("Hello %s %s", context.get(CONTEXT_PROPERTY1), context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceWithMissing",
                String.format("Testing ${UNKNOWN}, %s", context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceArray",
                String.format("[%s, %s, staticText]", context.get(CONTEXT_PROPERTY2), context.get(CONTEXT_PROPERTY3)));
        expectedModuleConfiguration.put("complexReferenceArrayWithMissing",
                String.format("[${UNKNOWN}, %s, staticText]", context.get(CONTEXT_PROPERTY3)));
        expectedModuleConfiguration.put("complexReferenceObj", String.format("{key1: %s, key2: staticText, key3: %s}",
                context.get(CONTEXT_PROPERTY1), context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceObjWithMissing",
                String.format("{key1: ${UNKNOWN}, key2: %s, key3: ${UNKNOWN2}}", context.get(CONTEXT_PROPERTY2)));

        // composite child module input with references
        compositeChildModuleInputsReferences.put("moduleInput", "$" + CONTEXT_PROPERTY1);
        compositeChildModuleInputsReferences.put("moduleInputMissing", "$UNKNOWN");
        compositeChildModuleInputsReferences.put("moduleInput2", "$" + CONTEXT_PROPERTY2);
        // expected resolved child module context
        expectedCompositeChildModuleContext.put("moduleInput", context.get(CONTEXT_PROPERTY1));
        expectedCompositeChildModuleContext.put("moduleInputMissing", context.get("UNKNOWN"));
        expectedCompositeChildModuleContext.put("moduleInput2", context.get(CONTEXT_PROPERTY2));
    }

    Logger log = LoggerFactory.getLogger(ReferenceResolverUtilTest.class);

    @Test
    public void testModuleConfigurationResolving() {
        // test trigger configuration..
        Module trigger = new Trigger(null, null, new HashMap<String, Object>(moduleConfiguration));
        ReferenceResolverUtil.updateModuleConfiguration(trigger, context);
        Assert.assertEquals(trigger.getConfiguration(), expectedModuleConfiguration);
        // test condition configuration..
        Module condition = new Condition(null, null, new HashMap<String, Object>(moduleConfiguration), null);
        ReferenceResolverUtil.updateModuleConfiguration(condition, context);
        Assert.assertEquals(condition.getConfiguration(), expectedModuleConfiguration);
        // test action configuration..
        Module action = new Action(null, null, new HashMap<String, Object>(moduleConfiguration), null);
        ReferenceResolverUtil.updateModuleConfiguration(action, context);
        Assert.assertEquals(action.getConfiguration(), expectedModuleConfiguration);
    }

    @Test
    public void testModuleInputResolving() {
        // test Composite child Module(condition) context
        Module condition = new Condition(null, null, null, compositeChildModuleInputsReferences);
        Map<String, Object> conditionContext = ReferenceResolverUtil.getCompositeChildContext(condition, context);
        Assert.assertEquals(conditionContext, expectedCompositeChildModuleContext);
        // test Composite child Module(action) context
        Module action = new Action(null, null, null, compositeChildModuleInputsReferences);
        Map<String, Object> actionContext = ReferenceResolverUtil.getCompositeChildContext(action, context);
        Assert.assertEquals(actionContext, expectedCompositeChildModuleContext);
    }
}