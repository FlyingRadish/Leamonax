package org.houxg.leamonax.widget.spinner;


import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpinnerArrayAdapter<T> extends ArrayAdapter<SpinnerArrayAdapter<T>.ItemProxy> {

    private NoFilter noFilter;

    public SpinnerArrayAdapter(Context context) {
        this(context, new ArrayList<T>());
    }

    public SpinnerArrayAdapter(Context context, List<T> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item);
        setNotifyOnChange(false);
        addAll(objects);
        notifyDataSetChanged();
    }

    public SpinnerArrayAdapter(Context context, T[] objects) {
        this(context, Arrays.asList(objects));
    }

    /**
     * <p>For default, the ArrayAdapter uses <code>toString()</code> for item view, in somes cases when uses a framework like Realm Database, is not possible to overrides the <code>toString()</code> in model classes.
     * Then, the proxy solutions has implemented.
     * <p>This method encapsulates the original object into a proxy.</p>
     *
     * @param objects
     * @return
     */
    private List<ItemProxy> wrapItems(List<T> objects) {
        List<ItemProxy> proxies = new ArrayList<>(objects.size());
        for (T item : objects) {
            ItemProxy proxy = new ItemProxy(item);
            proxies.add(proxy);
        }
        return proxies;
    }

    /**
     * <p>Converts the item to <code>String</code>.</p>
     * <p>Overrides this method for cutomize the text view.</p>
     *
     * @param item
     * @return
     */
    public String itemToString(T item) {
        return item.toString();
    }

    /**
     * <p>Converts a <code>String</code> in object.</p>
     * <p>This method can be used to get selected item.</p>
     * <p>Example:</p>
     * <code>
     * <pre>
     *
     * MaterialBetterSpinner spPeople;
     * SpinnerArrayAdapter<People> adapterPeople;
     * ....
     * People pSelected = adapterPeople.stringToItem(spPeople.getText())
     *
     * </pre>
     * </code>
     *
     * @param toString Selected text.
     * @return Object converted from selected text.
     */
    public T stringToItem(String toString) {
        for (int i = 0; i < getCount(); i++) {
            T item = getItem(i).object;
            if (itemToString(item).equals(toString)) {
                return item;
            }
        }
        return null;
    }

    /**
     * <p>Converts a <code>String</code> in object.</p>
     * <p>This method can be used to get selected item.</p>
     * <p>Example:</p>
     * <code>
     * <pre>
     *
     * MaterialBetterSpinner spPeople;
     * SpinnerArrayAdapter<People> adapterPeople;
     * ....
     * People pSelected = adapterPeople.stringToItem(spPeople.getText())
     *
     * </pre>
     * </code>
     *
     * @param toString Selected text.
     * @return Object converted from selected text.
     */
    public T stringToItem(CharSequence toString) {
        return stringToItem(toString.toString());
    }

    public void addAll(List<T> objects) {
        clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            addAll(wrapItems(objects));
        } else {
            for (ItemProxy p : wrapItems(objects)) {
                add(p);
            }
        }
    }

    /**
     * Proxy for generic SpinnerArrayAdapter.
     */
    public class ItemProxy {
        public final T object;

        protected ItemProxy(T object) {
            this.object = object;
        }

        @Override
        public String toString() {
            return itemToString(object);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SpinnerArrayAdapter.ItemProxy)) return false;

            ItemProxy itemProxy = (ItemProxy) o;

            return !(object != null ? !object.equals(itemProxy.object) : itemProxy.object != null);
        }

        @Override
        public int hashCode() {
            return object != null ? object.hashCode() : 0;
        }
    }

    /**
     * Override ArrayAdapter.getFilter() to return our own filtering.
     */
    @Override
    public Filter getFilter() {
        if (noFilter == null) {
            noFilter = new NoFilter();
        }
        return noFilter;
    }

    /**
     * Class which does not perform any filtering.
     * Filtering is already done by the web service when asking for the list,
     * so there is no need to do any more as well.
     * This way, ArrayAdapter.mOriginalValues is not used when calling e.g.
     * ArrayAdapter.add(), but instead ArrayAdapter.mObjects is updated directly
     * and methods like getCount() return the expected result.
     */
    private class NoFilter extends Filter {
        protected FilterResults performFiltering(CharSequence prefix) {
            return new FilterResults();
        }

        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            // Do nothing
        }
    }
}