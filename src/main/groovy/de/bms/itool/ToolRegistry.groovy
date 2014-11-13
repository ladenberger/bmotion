package de.bms.itool

import java.lang.ref.WeakReference

public class ToolRegistry {

    private final Map<String, ITool> tools = new HashMap<String, ITool>();
    List<WeakReference<IToolListener>> listeners = new ArrayList<WeakReference<IToolListener>>();

    public void registerListener(final IToolListener listener) {
        listeners.add(new WeakReference<IToolListener>(listener));
    }

    public void register(final String name, final ITool stateprovider) {
        tools.put(name, stateprovider);
        //notifyToolChange(stateprovider);
    }

    public void unregister(final String name) {
        tools.remove(name);
    }

    public void notifyToolChange(String trigger, final ITool tool) {
        for (WeakReference<IToolListener> listener : listeners) {
            listener.get().animationChange(trigger, tool);
        }
    }

    public ITool getTool(String id) {
        return tools.get(id);
    }

}
