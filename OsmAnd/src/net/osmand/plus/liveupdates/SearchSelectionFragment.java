package net.osmand.plus.liveupdates;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

import java.util.ArrayList;

public abstract class SearchSelectionFragment extends BaseOsmAndDialogFragment {
	private OnFragmentInteractionListener mListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_search_list, container, false);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		final ArrayAdapter<String> adapter = new ListAdapter(getActivity(), getListItemIcon());
		if (getArray() != null) {
			for (String s : getArray()) {
				adapter.add(s);
			}
		} else if (getList() != null) {
			for (String s : getList()) {
				adapter.add(s);
			}
		} else {
			throw new RuntimeException("Either getArray() or getList() must return non null value.");
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mListener.onSearchResult(adapter.getItem(position));
				dismiss();
			}
		});
		final EditText searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				adapter.getFilter().filter(s);
			}
		});
		ImageButton clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		setThemedDrawable(clearButton, R.drawable.ic_action_remove_dark);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else if (getParentFragment() instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) getParentFragment();
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	protected String[] getArray() {
		return null;
	}

	protected ArrayList<String> getList() {
		return null;
	}

	@DrawableRes
	protected int getListItemIcon() {
		return -1;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnFragmentInteractionListener {
		void onSearchResult(String name);
	}

	private class ListAdapter extends ArrayAdapter<String> {
		private final Drawable drawableLeft;

		public ListAdapter(Context context, @DrawableRes int drawableLeftId) {
			super(getMyActivity(), R.layout.osmand_simple_list_item_1);
			this.drawableLeft = drawableLeftId == -1 ? null : getContentIcon(drawableLeftId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView, parent);
			view.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
			view.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.list_content_padding));
			return view;
		}
	}
}
