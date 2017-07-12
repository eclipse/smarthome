package org.eclipse.smarthome.binding.mqtt.generic.internal.homie300;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.smarthome.binding.mqtt.generic.internal.mapping.FieldChanged;
import org.eclipse.smarthome.binding.mqtt.generic.internal.mapping.MqttTopicClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In homie there are some topics dedicated to list further subtopics. We need to watch those topics
 * and add/remove subnodes on a change. In homie 3.x these topics are meant to be watched:
 *
 * <pre>
 * * homie/mydevice/$nodes
 * * homie/mydevice/mynode/$properties
 * </pre>
 *
 * <p>
 * An example value of "homie/mydevice/$nodes" could be "lamp1,lamp2,switch", which means there are
 * "homie/mydevice/lamp1","homie/mydevice/lamp2" and "homie/mydevice/switch" existing.
 * </p>
 *
 * <p>
 * "lamp1,lamp2,switch" are each called an entity. The given {@link DeviceCallback} will be used to inform
 * about changed entities. On top of that, for each new entity a new Subscribable is created via the provided Supplier.
 * </p>
 *
 * @author David Graeff - Initial contribution
 *
 * @param <T> A {@link Subscribable}, usually a homie Device, Node or Property.
 */
public class SubtopicFieldObserver<T extends Subscribable> implements FieldChanged {
    private final Logger logger = LoggerFactory.getLogger(SubtopicFieldObserver.class);
    private final Map<String, Subscribable> map;
    private final Function<String, T> supplier;
    private final DeviceCallback callback;
    private final MqttTopicClassMapper topicMapper;
    private final int timeout;

    /**
     * Create a field observer. The field MUST be a String[].
     *
     * @param topicMapper
     * @param callback The callback is used to inform about removed entities
     *            ({@link DeviceCallback#subNodeRemoved(Subscribable)}) and if entities were added
     *            ({@link DeviceCallback#propertiesChanged()}).
     * @param map The state of the field is not exclusively saved in this object. Create a map
     *            entityID->SubscribableObject and provide it here. On an update of the MQTT topic,
     *            this map will be used to determine if an entity was added/removed. The map will be updated
     *            accordingly.
     * @param supplier On each new entity of the MQTT topic
     */
    @SuppressWarnings("unchecked")
    public SubtopicFieldObserver(MqttTopicClassMapper topicMapper, DeviceCallback callback, Map<String, T> map,
            Function<String, T> supplier, int timeout) {
        this.topicMapper = topicMapper;
        this.callback = callback;
        this.map = (Map<String, Subscribable>) map;
        this.supplier = supplier;
        this.timeout = timeout;
    }

    @Override
    public void fieldChanged(CompletableFuture<Boolean> future, String fieldname, Object value) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        Set<String> newSubnodes = new HashSet<>(Arrays.asList((String[]) value));
        for (String newSubnodeID : newSubnodes) {
            if (!map.containsKey(newSubnodeID)) {
                Subscribable newSubnode = supplier.apply(newSubnodeID);
                futures.add(newSubnode.subscribe(topicMapper, timeout).exceptionally(e -> {
                    logger.warn("Tried to add new property {}, but failed", newSubnodeID, e);
                    return null;
                }));
                map.put(newSubnodeID, newSubnode);
            }
        }
        for (Iterator<Map.Entry<String, Subscribable>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Subscribable> entry = it.next();
            if (!newSubnodes.contains(entry.getKey())) {
                Subscribable p = entry.getValue();
                p.unsubscribe(topicMapper);
                callback.subNodeRemoved(p);
                it.remove();
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenRun(() -> future.complete(true)).thenRun(() -> callback.propertiesChanged());
    }

}