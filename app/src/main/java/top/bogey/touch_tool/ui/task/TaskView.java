package top.bogey.touch_tool.ui.task;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.Usage;
import top.bogey.touch_tool.bean.save.SearchHistorySaver;
import top.bogey.touch_tool.bean.save.task.TaskSaveListener;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.ViewTagListItemBinding;
import top.bogey.touch_tool.databinding.ViewTaskBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.AnimUtil;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionItemRecyclerViewAdapter;
import top.bogey.touch_tool.ui.custom.dialog.EditTaskDialog;
import top.bogey.touch_tool.ui.tool.task_manager.ExportTaskDialog;
import top.bogey.touch_tool.ui.tool.task_manager.ImportTaskDialog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

public class TaskView extends Fragment implements TaskSaveListener {
    private ViewTaskBinding binding;
    private MenuItem searchMenuItem = null;

    boolean selecting = false;
    Set<String> selected = new HashSet<>();

    private TaskPageViewAdapter adapter;

    private final MenuProvider menuProvider = new MenuProvider() {

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_task, menu);
            searchMenuItem = menu.findItem(R.id.cleanSearch);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.cleanSearch) {
                binding.searchBar.setText("");
                resetTags();
            }
            return false;
        }
    };

    private final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (selecting) {
                unselectAll();
                hideBottomBar();
            } else {
                binding.searchView.hide();
            }
        }
    };

    @Override
    public void onDestroyView() {
        TaskSaver.getInstance().removeListener(this);
        binding.tasksBox.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        getParentFragmentManager().beginTransaction().attach(this).commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();
        activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        binding = ViewTaskBinding.inflate(inflater, container, false);

        binding.searchBar.addMenuProvider(menuProvider, getViewLifecycleOwner());

        TaskSaver.getInstance().addListener(this);

        TaskSearchItemAdapter searchAdapter = new TaskSearchItemAdapter(this);
        binding.searchBox.setAdapter(searchAdapter);

        binding.searchView.getEditText().addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    searchAdapter.setTasks("", new ArrayList<>());
                } else {
                    searchAdapter.setTasks(s.toString(), TaskSaver.getInstance().searchTasks(s.toString()));
                }
            }
        });

        binding.searchView.addTransitionListener((searchView, oldState, newState) -> callback.setEnabled(newState == SearchView.TransitionState.SHOWN || selecting));

        binding.searchView.getEditText().setOnEditorActionListener((textView, i, keyEvent) -> {
            Editable text = binding.searchView.getText();
            String textString = text.toString();
            SearchHistorySaver.getInstance().addSearchHistory(textString);
            refreshSearchHistory();
            binding.searchBar.setText(textString);
            resetTags();
            binding.searchView.hide();
            return false;
        });

        binding.cleanButton.setOnClickListener(v -> {
            SearchHistorySaver.getInstance().cleanSearchHistory();
            refreshSearchHistory();
        });
        refreshSearchHistory();

        binding.importButton.setOnClickListener(v -> activity.launcherOpenDocument((code, intent) -> {
            if (code == Activity.RESULT_OK && intent != null) {
                ImportTaskDialog.showDialog(activity, intent.getData());
            }
        }, "*/*"));

        adapter = new TaskPageViewAdapter(this);
        binding.tasksBox.setAdapter(adapter);
        new TabLayoutMediator(binding.tabBox, binding.tasksBox, (tab, position) -> tab.setText(adapter.tags.get(position))).attach();
        resetTags();

        binding.tagButton.setOnClickListener(v -> new TagView(requireContext(), this).show());

        binding.selectAllButton.setOnClickListener(v -> selectAll());

        binding.deleteButton.setOnClickListener(v -> {
            List<Usage> usages = new ArrayList<>();
            for (String id : selected) {
                usages.addAll(TaskSaver.getInstance().getTaskUses(id));
            }
            if (!usages.isEmpty()) {
                LinearLayout linearLayout = SelectActionItemRecyclerViewAdapter.getTipsLinearLayout(requireContext(), usages, R.string.task_delete_tips);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.remove_task)
                        .setView(linearLayout)
                        .setPositiveButton(R.string.enter, null)
                        .setNegativeButton(R.string.force_delete, (dialog, which) -> {
                            for (String id : selected) {
                                TaskSaver.getInstance().removeTask(id);
                            }
                            hideBottomBar();
                        })
                        .show();

                int px = (int) DisplayUtil.dp2px(requireContext(), 32);
                DisplayUtil.setViewMargin(linearLayout, px, px / 2, px, px / 2);
            } else {
                AppUtil.showDialog(requireContext(), R.string.remove_tips, result -> {
                    if (result) {
                        for (String id : selected) {
                            TaskSaver.getInstance().removeTask(id);
                        }
                        hideBottomBar();
                    }
                });
            }
        });

        binding.exportButton.setOnClickListener(v -> {
            List<Task> tasks = new ArrayList<>();
            selected.forEach(id -> {
                Task task = TaskSaver.getInstance().getTask(id);
                if (task == null) return;
                tasks.add(task);
            });
            ExportTaskDialog.showDialog(requireContext(), tasks);

            unselectAll();
            hideBottomBar();
        });

        binding.moveButton.setOnClickListener(v -> new TagView(requireContext(), this).show());

        binding.copyButton.setOnClickListener(v -> {
            selected.forEach(id -> {
                Task task = TaskSaver.getInstance().getTask(id);
                if (task == null) return;
                Task copy = task.newCopy();
                copy.setTitle(getString(R.string.copy_title, task.getTitle()));
                copy.save();
            });

            unselectAll();
            hideBottomBar();
        });

        binding.addButton.setOnClickListener(v -> {
            String currentTag = getCurrentTag();
            Task task = new Task();
            task.addTag(currentTag);
            EditTaskDialog dialog = new EditTaskDialog(requireContext(), task);
            dialog.setTitle(R.string.task_add);
            dialog.setCallback(result -> {
                if (result) task.save();
            });
            dialog.show();
        });

        return binding.getRoot();
    }

    public void resetTags() {
        CharSequence text = binding.searchBar.getText();
        String string = text.toString();
        if (string.isEmpty()) {
            List<String> tags = TaskSaver.getInstance().getTaskTags();
            adapter.setTags(tags);
            if (searchMenuItem != null) searchMenuItem.setVisible(false);
        } else {
            adapter.search(string);
            if (searchMenuItem != null) searchMenuItem.setVisible(true);
        }
    }

    public void refreshSearchHistory() {
        binding.historyBox.removeAllViews();
        for (String history : SearchHistorySaver.getInstance().getSearchHistory()) {
            ViewTagListItemBinding itemBinding = ViewTagListItemBinding.inflate(LayoutInflater.from(getContext()), binding.historyBox, true);
            Chip chip = itemBinding.getRoot();
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                SearchHistorySaver.getInstance().removeSearchHistory(history);
                binding.historyBox.removeView(chip);
            });

            chip.setCheckable(false);
            chip.setText(history);

            chip.setOnClickListener(v -> binding.searchView.getEditText().setText(history));
        }
    }

    public String getCurrentTag() {
        TabLayout.Tab tab = binding.tabBox.getTabAt(binding.tabBox.getSelectedTabPosition());
        if (tab == null || tab.getText() == null) return null;
        return tab.getText().toString();
    }

    public void gotoTargetTag(String tag) {
        for (int i = 0; i < binding.tabBox.getTabCount(); i++) {
            TabLayout.Tab tab = binding.tabBox.getTabAt(i);
            if (tab == null || tab.getText() == null) continue;
            if (tag.equals(tab.getText().toString())) {
                binding.tabBox.selectTab(tab);
            }
        }
    }

    public void showBottomBar() {
        MainApplication.getInstance().getActivity().hideBottomNavigation();

        // 添加按钮先下沉收走，选择工具栏再浮上来
        AnimUtil.swapSequential(binding.addButton, 16f, binding.bottomBar, 16f);

        selecting = true;
        selected.clear();
        callback.setEnabled(true);
    }

    public void hideBottomBar() {
        MainApplication.getInstance().getActivity().showBottomNavigation();

        // 选择工具栏先下沉收走，添加按钮再浮上来
        AnimUtil.swapSequential(binding.bottomBar, 16f, binding.addButton, 16f);

        selecting = false;
        selected.clear();
        callback.setEnabled(false);
    }

    public void selectAll() {
        TabLayout.Tab tab = binding.tabBox.getTabAt(binding.tabBox.getSelectedTabPosition());
        if (tab == null || tab.getText() == null) return;

        String tag = tab.getText().toString();
        List<Task> tasks = TaskSaver.getInstance().getTasks(tag);

        boolean flag = true;
        if (selected.size() == tasks.size()) {
            boolean matched = true;
            for (Task task : tasks) {
                if (!selected.contains(task.getId())) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                flag = false;
            }
        }

        if (flag) {
            tasks.forEach(task -> selected.add(task.getId()));
            adapter.notifyItemChanged(binding.tabBox.getSelectedTabPosition());
        } else {
            unselectAll();
        }
    }

    public void unselectAll() {
        selected.clear();
        adapter.notifyItemChanged(binding.tabBox.getSelectedTabPosition());
    }

    @Override
    public void onCreate(Task task) {
        AppUtil.runOnUiThread(this::resetTags);
    }

    @Override
    public void onUpdate(Task task) {
        AppUtil.runOnUiThread(this::resetTags);
    }

    @Override
    public void onRemove(Task task) {
        AppUtil.runOnUiThread(this::resetTags);
    }
}
