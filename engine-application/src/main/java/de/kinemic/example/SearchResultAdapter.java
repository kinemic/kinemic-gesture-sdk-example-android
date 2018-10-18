package de.kinemic.example;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.kinemic.example.SearchResultAdapter.MutableSearchResult;
import de.kinemic.gesture.SearchCallback;
import de.kinemic.gesture.SearchResult;
import java.util.Collection;
import java.util.Comparator;


/**
 * List adapter to manage search results.
 *
 * Implements SearchCallback and thus should be used in
 * {@link de.kinemic.gesture.Engine#startSearch(de.kinemic.gesture.SearchCallback)}
 */
public class SearchResultAdapter extends ArrayAdapter<MutableSearchResult> implements SearchCallback {

    /** Small wrapper around SearchResult, since SearchResult is immutable and we want to update its rssi */
    public static class MutableSearchResult {

        public short rssi;
        public final @NonNull String address;

        public MutableSearchResult(final @NonNull String address, final short rssi) {
            this.address = address;
            this.rssi = rssi;
        }

        public static MutableSearchResult from(SearchResult searchResult) {
            return new MutableSearchResult(searchResult.getMacaddress(), searchResult.getRssi());
        }
    }

    private final Context context;
    @LayoutRes private final int itemLayout;
    private boolean sorting = false;

    public SearchResultAdapter(@NonNull Context context) {
        this(context, R.layout.search_result_item);
    }

    public void setSorting(boolean sorting) {
        this.sorting = sorting;
    }

    public SearchResultAdapter(@NonNull Context context, @LayoutRes int itemLayout) {
        super(context, itemLayout);
        this.context = context;
        this.itemLayout = itemLayout;
    }

    @Override
    public void onBandFound(SearchResult sensor) {
        update(sensor);
    }

    @Override
    public void onSearchStarted() {

    }

    @Override
    public void onSearchStopped() {

    }

    static class ViewHolder {
        TextView address;
        TextView rssi;

        ViewHolder(View view) {
            address = view.findViewById(de.kinemic.gesture.R.id.sensor_address);
            rssi = view.findViewById(de.kinemic.gesture.R.id.sensor_rssi);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(this.itemLayout, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        MutableSearchResult result = getItem(position);
        viewHolder.address.setText(result.address);
        viewHolder.rssi.setText(String.format("rssi: %d", result.rssi));

        return convertView;
    }

    /** update list with SearchResult, only update rssi if already in list */
    private void update(@Nullable SearchResult object) {
        for (int i = 0; i < getCount(); ++i) {
            MutableSearchResult item = getItem(i);
            if (item.address.equals(object.getMacaddress())) {
                item.rssi = object.getRssi();

                if (sorting) sort((lhs, rhs) -> rhs.rssi - lhs.rssi);
                notifyDataSetChanged();
                return;
            }
        }

        super.add(MutableSearchResult.from(object));
        if (sorting) sort((lhs, rhs) -> rhs.rssi - lhs.rssi);
    }

    @Override
    public void add(@Nullable MutableSearchResult object) {
        throw new IllegalStateException("Do not add items manual, use the adapter as a SearchCallback");
    }

    @Override
    public void addAll(MutableSearchResult... items) {
        throw new IllegalStateException("Do not add items manual, use the adapter as a SearchCallback");
    }

    @Override
    public void addAll(@NonNull Collection<? extends MutableSearchResult> collection) {
        throw new IllegalStateException("Do not add items manual, use the adapter as a SearchCallback");
    }
}
