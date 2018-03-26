package ru.esdev.vcalendar.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

import timber.log.Timber;

import static ru.esdev.vcalendar.common.Preconditions.checkNotNull;


/**
 * Mds. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class MultiRowAdapter extends RecyclerView.Adapter<MultiRowAdapter.RowViewHolder> {

    //@TODO может weak ссылки хранить?
    protected List<MultiRowContract.Row> mItems = new ArrayList<>();
    private SimpleArrayMap<Integer, Class<? extends RowViewHolder>> holderViewIdClassCache = new SimpleArrayMap<>();
    private WeakHashMap<Integer, MultiRowContract.Row<?>> rowsViewIdClassCache = new WeakHashMap<>();
    private LayoutInflater layoutInflater;
    private boolean mEnableSorting = true;

    public MultiRowAdapter(Builder builder) {
        this(builder.mRows);
        mEnableSorting = builder.mEnableSort;
        if (mEnableSorting) {
            sort();
        }
    }

    public MultiRowAdapter(@NonNull ArrayList<MultiRowContract.Row> items) {
        mItems = checkNotNull(items, "Rows can't be a null");
        makeHoldersCache();
    }

    public MultiRowAdapter() {
    }

    public void updateRows(List<MultiRowContract.Row<?>> newRows) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(newRows), true);
        diffResult.dispatchUpdatesTo(this);
    }

    public <V extends MultiRowAdapter.RowViewHolder, T extends MultiRowContract.Row<V>>
    MultiRowAdapter addRow(T row, int position) {
        if (row == null || !row.isVisible()) {
            return this;
        }

        int beforeSize = mItems.size();
        final SortableRow<V, T> newRow = new SortableRow<>(row, position);
        if (mItems.contains(newRow)) {
            Timber.d("Updating row: %s with position %d", row.getClass().getSimpleName(), position);
            final int idx = mItems.indexOf(newRow);
            mItems.set(idx, newRow);
            notifyItemChanged(idx);
            return this;
        }
        Timber.d("Adding row: %s with position %d", row.getClass().getSimpleName(), position);

        mItems.add(newRow);

        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        if (beforeSize == 0) {
            notifyItemInserted(0);
        } else {
            notifyItemRangeChanged(0, mItems.size());
        }
        return this;
    }

	public void addRowsTop(MultiRowContract.Row[] rows) {
		addRowsTop(Stream.of(rows).toList());
	}

    @SuppressWarnings("Convert2MethodRef")
    public void addRowsTop(Collection<MultiRowContract.Row> rows) {
        if (rows.isEmpty()) return;

        final List<MultiRowContract.Row> target = Stream.of(rows)
                .filter(item -> item != null)
                .filter(MultiRowContract.Row::isVisible)
                .toList();

        mItems.addAll(0, target);
        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        notifyItemRangeInserted(0, target.size());
    }

    @SuppressWarnings("Convert2MethodRef")
    public void addRows(MultiRowContract.Row[] rows) {
        if (rows.length == 0) return;

        final List<MultiRowContract.Row> target = Stream.of(rows)
                .filter(item -> item != null)
                .filter(MultiRowContract.Row::isVisible)
                .toList();

        int beforeSize = mItems.size();
        mItems.addAll(target);
        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        notifyItemRangeInserted(beforeSize, target.size());
    }

    @SuppressWarnings("Convert2MethodRef")
    public void addRows(Collection<MultiRowContract.Row> rows) {
        if (rows.isEmpty()) return;

        final List<MultiRowContract.Row> target = Stream.of(rows)
                .filter(item -> item != null)
                .filter(MultiRowContract.Row::isVisible)
                .toList();

        int beforeSize = mItems.size();
        mItems.addAll(target);
        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        notifyItemRangeInserted(beforeSize, target.size());
    }

    public void addRowTop(MultiRowContract.Row row) {
        if (row == null || !row.isVisible()) {
            return;
        }
        mItems.add(0, row);
        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        notifyItemInserted(mItems.size());
    }

    public void addRow(MultiRowContract.Row row) {
        if (row == null || !row.isVisible()) {
            return;
        }
        mItems.add(row);
        if (mEnableSorting) {
            sort();
        }

        makeHoldersCache();
        notifyItemInserted(mItems.size());
    }

    public void setEnableSorting(boolean enableSorting) {
        mEnableSorting = enableSorting;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final String tag = String.format("ViewHolder instancing: %s", parent.getContext().getResources().getResourceEntryName(viewType));
//        TimeProfiler.start(tag);

        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }

        if (viewType == View.NO_ID || viewType == 0) {
            throw new RuntimeException("Layout id can't be 0");
        }

        View v = layoutInflater.inflate(viewType, parent, false);

        RowViewHolder viewHolder = null;
        Throwable cause = null;
        try {
            viewHolder = findViewHolder(viewType, v);
        } catch (NoSuchMethodException e) {
            cause = e;
            Timber.e(e, "Error finding ViewHolder");
        } catch (IllegalAccessException e) {
            cause = e;
            Timber.e(e, "Error finding ViewHolder");
        } catch (InvocationTargetException e) {
            cause = e;
            Timber.e(e, "Error finding ViewHolder");
        } catch (InstantiationException e) {
            cause = e;
            Timber.e(e, "Error finding ViewHolder");
        }

        if (viewHolder == null) {
            throw new RuntimeException(cause);
        }

        return viewHolder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        MultiRowContract.Row item = getItemByPosition(position);
        item.onBindViewHolder(holder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewDetachedFromWindow(@NonNull RowViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();
        if (position < 0) {
            return;
        }

        MultiRowContract.Row item = getItemByPosition(position);
        item.onUnbindViewHolder(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return getItemByPosition(position).getItemView();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void clear() {
        if (mItems.isEmpty()) return;
        mItems.clear();
        notifyDataSetChanged();
        holderViewIdClassCache.clear();
    }

    public void clearNotify() {
        final int size = mItems.size();
        if (size == 0) return;

        notifyItemRangeRemoved(0, size);
        mItems.clear();
    }

    /**
     * Nullptr safe remove row
     *
     * @param row MultiRowContract.Row
     * @param <T> Result
     */
    public <T extends MultiRowContract.Row> void remove(T row) {
        if (row == null) return;
        if (mItems.contains(row)) {
            int index = mItems.indexOf(row);
            mItems.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void removeUniqueLayout(@LayoutRes int rowLayout) {
        if (rowLayout == View.NO_ID) return;

        for (MultiRowContract.Row r : mItems) {
            if (r.getItemView() == rowLayout) {
                remove(r);
                return;
            }
        }
    }

    public <T extends MultiRowContract.Row> void removeUniqueLayout(T row) {
        if (row == null) return;

        for (MultiRowContract.Row r : mItems) {
            if (r.getItemView() == row.getItemView()) {
                remove(r);
                return;
            }
        }
    }

    public MultiRowContract.Row getItemByPosition(int position) {
        return mItems.get(position);
    }

    public <T> Stream<T> findStream(Class<T> rowClass) {
        return Stream.of(mItems)
                .filter(item -> {
                    if (item instanceof SortableRow) {
                        return rowClass.isInstance(item) || rowClass.isInstance(((SortableRow) item).getRow());
                    }

                    return rowClass.isInstance(item);
                })
                .map(rowClass::cast);
    }

    public void sort(Comparator<MultiRowContract.Row> c) {
        Collections.sort(mItems, c);
    }

    public void sort() {
        Collections.sort(mItems, new RowComparator());
    }


    /**
     * Кэшированный список классов ViewHolder'ов чтоб итеративно каждый раз не искать
     */
    @SuppressWarnings("unchecked")
    protected void makeHoldersCache() {
        checkNotNull(mItems, "Wow! Rows can't be null");
        holderViewIdClassCache.clear();
        holderViewIdClassCache = new SimpleArrayMap<>(mItems.size());
        rowsViewIdClassCache = new WeakHashMap<>(mItems.size());

        for (MultiRowContract.Row item : mItems) {
            checkNotNull(item);
            if (item instanceof SortableRow) {
                checkNotNull(item.getViewHolderClass(),
                        "Row " + (((SortableRow) item).getRow().getClass()) + " does not have valid ViewHolder class");
            } else {
                checkNotNull(item.getViewHolderClass(),
                        "Row " + item.getClass() + " does not have valid ViewHolder class");
            }

            rowsViewIdClassCache.put(item.getItemView(), item);
            holderViewIdClassCache.put(item.getItemView(), item.getViewHolderClass());
        }
    }

    private boolean isInnerClass(Class<?> clazz) {
        return clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers());
    }

    private MultiRowContract.Row<?> findRow(@LayoutRes int viewId) {
        final MultiRowContract.Row<?> row = rowsViewIdClassCache.get(viewId);
        if (row == null) {
            makeHoldersCache();
        }

        return rowsViewIdClassCache.get(viewId);
    }

    @NonNull
    private RowViewHolder findViewHolder(@LayoutRes int viewId, View view)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends RowViewHolder> holderClass = holderViewIdClassCache.get(viewId);
        if (holderClass == null) {
            makeHoldersCache();
        }
        holderClass = holderViewIdClassCache.get(viewId);
        if (holderClass == null) {
            throw new RuntimeException("Can't findStream ViewHolder for view " + String.valueOf(viewId));
        }
        if (isInnerClass(holderClass)) {
            throw new RuntimeException("Class should be static!");
        }
        return holderClass.getDeclaredConstructor(View.class).newInstance(view);
    }

    public static class Builder {

        private ArrayList<MultiRowContract.Row> mRows = new ArrayList<>();
        private boolean mEnableSort;

        public Builder addRow(MultiRowContract.Row row) {
            mRows.add(row);
            return this;
        }

        public Builder enableSort(boolean enable) {
            mEnableSort = enable;
            return this;
        }

        public MultiRowAdapter build() {
            if (mEnableSort) {

            }
            return new MultiRowAdapter(this);
        }
    }

    public static abstract class RowViewHolder extends RecyclerView.ViewHolder {

        public RowViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class RowComparator implements Comparator<MultiRowContract.Row> {

        @Override
        public int compare(MultiRowContract.Row o1, MultiRowContract.Row o2) {
            return o1.getRowPosition() - o2.getRowPosition();
        }
    }

    final class DiffCallback extends DiffUtil.Callback {
        List<MultiRowContract.Row<?>> mRows;

        DiffCallback(List<MultiRowContract.Row<?>> rows) {
            mRows = rows;
        }

        @Override
        public int getOldListSize() {
            return mItems.size();
        }

        @Override
        public int getNewListSize() {
            return mRows.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mItems.get(oldItemPosition).getItemView() == mRows.get(newItemPosition).getItemView()
                    && mItems.get(oldItemPosition).getRowPosition() == mRows.get(newItemPosition).getRowPosition();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mItems.get(oldItemPosition).equals(mRows.get(newItemPosition));
        }
    }
}