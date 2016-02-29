/*
 * Copyright (C) 2016 based on The Android Open Source Project HeaderGridView
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
import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.WrapperListAdapter;

/**
 * 
 * @author Ahmed Basyouni
 * 
 *         listView that support adding banner as first item or not as it will
 *         act as a normal listView to use this class all you have to do is to
 *         pass a fragment or a normal view as a banner and that is it
 * 
 */

public class BannerListView extends ListView {

	// id for fragment adding to view
	private static final int CONTAINER_ID = 1215;

	// array of headers
	private FixedViewInfo mBannerView;

	// init views
	public BannerListView(Context context) {
		super(context);
	}

	public BannerListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BannerListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * 
	 * @author Ahmed Basyouni 
	 * 
	 * 		   subclass of frameLayout to get full width of screen
	 *         since banner will be full width
	 * 
	 */
	private class FullWidthFixedViewLayout extends FrameLayout {
		public FullWidthFixedViewLayout(Context context) {
			super(context);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int targetWidth = this.getMeasuredWidth() - this.getPaddingLeft()
					- this.getPaddingRight();
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
					MeasureSpec.getMode(widthMeasureSpec));
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	/**
	 * this method take supported fragment and fragment activity to add that
	 * fragment as a banner to listView it create a layout at runtime then when
	 * view is added to list we replace it with fragment otherwise it will crash
	 * since view is not on screen to be replaced
	 * 
	 * @param fragment
	 * @param activity
	 */
	public void addBannerFragment(final Fragment fragment,
			final FragmentActivity activity) {

		RelativeLayout layout = new RelativeLayout(activity);

		AbsListView.LayoutParams param = new AbsListView.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

		layout.setLayoutParams(param);

		layout.setId(CONTAINER_ID);

		addLayoutContainer(layout);

		this.addOnLayoutChangeListener(new OnLayoutChangeListener() {

			@Override
			public void onLayoutChange(View v, int left, int top, int right,
					int bottom, int oldLeft, int oldTop, int oldRight,
					int oldBottom) {

				BannerListView.this.removeOnLayoutChangeListener(this);

				FragmentManager manager = activity.getSupportFragmentManager();
				FragmentTransaction transaction = manager.beginTransaction();

				transaction.add(CONTAINER_ID, fragment).commit();
			}
		});

	}

	/**
	 * this method a view to be added as a banner to listView
	 * 
	 */
	public void addHeaderView(View v) {

		addLayoutContainer(v);

	}

	/**
	 * a method that take view and add it to list in case we add fragment it
	 * take a linear layout as a container to fragment else it will leave the
	 * passed view as it is
	 * 
	 * @param view
	 */
	private void addLayoutContainer(View view) {

		ListAdapter adapter = getAdapter();
		if (adapter != null && !(adapter instanceof HeaderViewListAdapter)) {
			throw new IllegalStateException(
					"Cannot add header view to grid -- setAdapter has already been called.");
		}

		FixedViewInfo info = new FixedViewInfo();
		FrameLayout fl = new FullWidthFixedViewLayout(getContext());
		fl.addView(view);
		info.view = view;
		mBannerView = info;
		// mHeaderViewInfos.add(info);
		// in the case of re-adding a header view, or adding one later on,
		// we need to notify the observer
		if (adapter != null) {
			((HeaderViewListAdapter) adapter).notifyDataSetChanged();
		}
	}

	/**
	 * 
	 * @return 1 if there is a header else return 0
	 */
	public int getHeaderViewCount() {
		return mBannerView != null ? 1 : 0;
	}

	/**
	 * this method take the passed adapter and check if there is a header it
	 * will pass adapter to another custom adapter else it will act as a normal
	 * listView
	 */
	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mBannerView != null) {
			HeaderViewListAdapter hadapter = new HeaderViewListAdapter(
					mBannerView, adapter);
			super.setAdapter(hadapter);
		} else {
			super.setAdapter(adapter);
		}
	}

	/**
	 * 
	 * @author Ahmed Basyouni
	 * 
	 *         custom adapter that take a banner and an adapter if there is
	 *         header it will add it as first view and return the passed adapter
	 *         for all other positions
	 */
	private static class HeaderViewListAdapter implements WrapperListAdapter,
			Filterable {

		private final DataSetObservable mDataSetObservable = new DataSetObservable();
		private final ListAdapter mAdapter;
		// This ArrayList is assumed to NOT be null.
		FixedViewInfo mHeader;
		private final boolean mIsFilterable;

		public HeaderViewListAdapter(FixedViewInfo headerViewInfos,
				ListAdapter adapter) {
			mAdapter = adapter;
			mIsFilterable = adapter instanceof Filterable;
			if (headerViewInfos == null) {
				throw new IllegalArgumentException(
						"headerViewInfos cannot be null");
			}
			mHeader = headerViewInfos;
		}

		public int getHeadersCount() {
			return mHeader != null ? 1 : 0;
		}

		@Override
		public boolean areAllItemsEnabled() {

			if (mAdapter != null) {
				return mAdapter.areAllItemsEnabled();
			} else {
				return true;
			}
		}

		@Override
		public boolean isEnabled(int position) {

			int numHeadersAndPlaceholders = getHeadersCount();
			if (position < numHeadersAndPlaceholders) {
				return true;
			}
			// Adapter
			final int adjPosition = position - numHeadersAndPlaceholders;
			int adapterCount = 0;
			if (mAdapter != null) {
				adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.isEnabled(adjPosition);
				}
			}
			throw new ArrayIndexOutOfBoundsException(position);

		}

		/**
		 * return total numbers of items (header + passed adapter count) else
		 * return number of header only
		 */
		@Override
		public int getCount() {

			if (mAdapter != null) {
				return getHeadersCount() + mAdapter.getCount();
			} else {
				return getHeadersCount();
			}

		}

		/**
		 * return item at that position if position == 0 it will return header
		 * else it will adjust position (i.e position - 1) and return that item
		 */
		@Override
		public Object getItem(int position) {

			int numHeadersAndPlaceholders = getHeadersCount();
			if (position < numHeadersAndPlaceholders) {
				if (position == 0) {
					return mHeader.data;
				}
				return null;
			}
			// Adapter
			final int adjPosition = position - numHeadersAndPlaceholders;
			int adapterCount = 0;
			if (mAdapter != null) {
				adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getItem(adjPosition);
				}
			}
			throw new ArrayIndexOutOfBoundsException(position);
		}

		@Override
		public long getItemId(int position) {

			int numHeadersAndPlaceholders = getHeadersCount();
			if (mAdapter != null && position >= numHeadersAndPlaceholders) {
				int adjPosition = position - numHeadersAndPlaceholders;
				int adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getItemId(adjPosition);
				}
			}
			return -1;

		}

		@Override
		public int getItemViewType(int position) {

			int numHeadersAndPlaceholders = getHeadersCount();
			if (position < numHeadersAndPlaceholders) {

				return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
			}
			if (mAdapter != null && position >= numHeadersAndPlaceholders) {
				int adjPosition = position - numHeadersAndPlaceholders;
				int adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getItemViewType(adjPosition);
				}
			}
			return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			int numHeadersAndPlaceholders = getHeadersCount();
			if (position < numHeadersAndPlaceholders) {
				View headerViewContainer = mHeader.view;
				if (position == 0) {
					return headerViewContainer;
				}
			}
			// Adapter
			final int adjPosition = position - numHeadersAndPlaceholders;
			int adapterCount = 0;
			if (mAdapter != null) {
				adapterCount = mAdapter.getCount();
				if (adjPosition < adapterCount) {
					return mAdapter.getView(adjPosition, convertView, parent);
				}
			}
			throw new ArrayIndexOutOfBoundsException(position);
		}

		@Override
		public int getViewTypeCount() {

			if (mAdapter != null) {
				return mAdapter.getViewTypeCount() + 1;
			}
			return 2;
		}

		@Override
		public boolean hasStableIds() {
			if (mAdapter != null) {
				return mAdapter.hasStableIds();
			}
			return false;
		}

		@Override
		public boolean isEmpty() {
			return (mAdapter == null || mAdapter.isEmpty())
					&& getHeadersCount() == 0;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.registerObserver(observer);
			if (mAdapter != null) {
				mAdapter.registerDataSetObserver(observer);
			}
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.unregisterObserver(observer);
			if (mAdapter != null) {
				mAdapter.unregisterDataSetObserver(observer);
			}
		}

		@Override
		public Filter getFilter() {
			if (mIsFilterable) {
				return ((Filterable) mAdapter).getFilter();
			}
			return null;
		}

		@Override
		public ListAdapter getWrappedAdapter() {
			return mAdapter;
		}

		public void notifyDataSetChanged() {
			mDataSetObservable.notifyChanged();
		}

	}

}
