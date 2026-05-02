package ceui.lisa.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.FragmentFilterBinding;
import ceui.lisa.utils.Common;
import ceui.lisa.viewmodel.SearchModel;

import ceui.lisa.utils.PixivSearchParamUtil;

public class FragmentFilter extends BaseFragment<FragmentFilterBinding> {

    private SearchModel searchModel;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        searchModel = new ViewModelProvider(requireActivity()).get(SearchModel.class);
        searchModel.getIsNovel().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                initTagSpinner(aBoolean);
            }
        });
        searchModel.getStartDate().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                baseBind.startDate.setText(TextUtils.isEmpty(s) ? getString(R.string.string_330) : s);
            }
        });
        searchModel.getEndDate().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                baseBind.endDate.setText(TextUtils.isEmpty(s) ? getString(R.string.string_330) : s);
            }
        });
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_filter;
    }

    @Override
    public void initView() {
        baseBind.submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });

        initTagSpinner(false);

        ArrayAdapter<String> starAdapter = new ArrayAdapter<>(mContext,
                R.layout.spinner_item, PixivSearchParamUtil.ALL_SIZE_NAME);
        baseBind.starSizeSpinner.setAdapter(starAdapter);
        baseBind.starSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                searchModel.getStarSize().setValue(PixivSearchParamUtil.ALL_SIZE_VALUE[position]);
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        for (int i = 0; i < PixivSearchParamUtil.ALL_SIZE_VALUE.length; i++) {
            if (PixivSearchParamUtil.ALL_SIZE_VALUE[i].equals(Shaft.sSettings.getSearchFilter())) {
                baseBind.starSizeSpinner.setSelection(i);
                break;
            }
        }

        ArrayAdapter<String> sortTypeAdapter = new ArrayAdapter<>(mContext,
                R.layout.spinner_item, PixivSearchParamUtil.SORT_TYPE_NAME);
        baseBind.sortTypeSpinner.setAdapter(sortTypeAdapter);
        baseBind.sortTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                searchModel.getSortType().setValue(PixivSearchParamUtil.SORT_TYPE_VALUE[position]);
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        searchModel = new ViewModelProvider(requireActivity()).get(SearchModel.class);
        String initialSort = searchModel.getSortType().getValue();
        baseBind.sortTypeSpinner.setSelection(
                PixivSearchParamUtil.getSortTypeIndex(
                        initialSort != null ? initialSort : Shaft.sSettings.getSearchDefaultSortType()));

        baseBind.startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDatePicker(searchModel.getStartDate());
            }
        });
        baseBind.endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDatePicker(searchModel.getEndDate());
            }
        });
        baseBind.startEndDateClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchModel.getStartDate().setValue(null);
                searchModel.getEndDate().setValue(null);
                performSearch();
            }
        });

        /*if (SessionManager.INSTANCE.isPremium()) {
            baseBind.popSwitch.setEnabled(true);
        } else {
            baseBind.popSwitch.setEnabled(false);
        }
        baseBind.popSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    searchModel.getSortType().setValue(PixivSearchParamUtil.POPULAR_SORT_VALUE);
                } else {
                    searchModel.getSortType().setValue("");
                }
            }
        });*/
        baseBind.restrictionToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int value;
                if (checkedId == R.id.restriction_btn_0) {
                    value = 0;
                } else if (checkedId == R.id.restriction_btn_1) {
                    value = 1;
                } else {
                    value = 2;
                }
                searchModel.getR18Restriction().setValue(value);
                performSearch();
            }
        });

    }

    private void initTagSpinner(boolean isNovel) {
        String[] titles = isNovel ? PixivSearchParamUtil.TAG_MATCH_NAME_NOVEL : PixivSearchParamUtil.TAG_MATCH_NAME;
        String[] values = isNovel ? PixivSearchParamUtil.TAG_MATCH_VALUE_NOVEL : PixivSearchParamUtil.TAG_MATCH_VALUE;
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(mContext,
                R.layout.spinner_item, titles);
        baseBind.tagSpinner.setAdapter(tagAdapter);
        baseBind.tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(searchModel != null && searchModel.getSearchType().getValue() != null
                        && searchModel.getSearchType().getValue().equals(values[position])){
                    return;
                }
                searchModel.getSearchType().setValue(values[position]);
                performSearch();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if(searchModel != null){
            int index = Arrays.asList(values).indexOf(searchModel.getSearchType().getValue());
            baseBind.tagSpinner.setSelection(Math.max(index, 0));
        }
    }

    private void setDatePicker(MutableLiveData<String> dateData) {
        String currentDate = dateData.getValue();

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar);

        if (!TextUtils.isEmpty(currentDate)) {
            String[] t = currentDate.split("-");
            Calendar initial = Calendar.getInstance();
            initial.set(Integer.parseInt(t[0]), Integer.parseInt(t[1]) - 1, Integer.parseInt(t[2]));
            builder.setSelection(initial.getTimeInMillis());
        }

        MaterialDatePicker<Long> datePicker = builder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            String date = LocalDate.of(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)).toString();
            dateData.setValue(date);
            performSearch();
        });

        datePicker.show(getParentFragmentManager(), "DatePickerDialog");
    }

    private void performSearch(){
        searchModel.getNowGo().setValue("search_now");
    }
}
