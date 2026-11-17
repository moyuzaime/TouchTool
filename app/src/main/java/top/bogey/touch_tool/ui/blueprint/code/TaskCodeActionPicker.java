package top.bogey.touch_tool.ui.blueprint.code;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.databinding.DialogTaskCodeActionBinding;
import top.bogey.touch_tool.databinding.ItemTaskCodeActionBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

// 动作模板选择器：只挑选类型，模板由 TaskCode.newAction 用真实动作类生成
public class TaskCodeActionPicker {
    public interface Listener {
        void onPick(ActionType type);
    }

    public static void show(Context context, Listener listener) {
        DialogTaskCodeActionBinding binding = DialogTaskCodeActionBinding.inflate(LayoutInflater.from(context));

        List<ActionType> all = new ArrayList<>();
        for (ActionType type : ActionType.values()) {
            if (ActionInfo.getActionInfo(type) != null) all.add(type);
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        all.sort((left, right) -> collator.compare(displayTitle(ActionInfo.getActionInfo(left), left), displayTitle(ActionInfo.getActionInfo(right), right)));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_action_pick)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .create();

        Adapter adapter = new Adapter(all, type -> {
            dialog.dismiss();
            listener.onPick(type);
        });
        binding.actionList.setLayoutManager(new LinearLayoutManager(context));
        binding.actionList.setAdapter(adapter);

        binding.actionSearch.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                String query = s == null ? "" : s.toString().trim();
                List<ActionType> filtered = new ArrayList<>();
                for (ActionType type : all) {
                    ActionInfo info = ActionInfo.getActionInfo(type);
                    String description = info.getDescription();
                    if (query.isEmpty()
                            || AppUtil.isStringContainsWithPinyin(displayTitle(info, type), query)
                            || (description != null && AppUtil.isStringContains(description, query))) {
                        filtered.add(type);
                    }
                }
                adapter.submit(filtered);
                binding.actionEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (DisplayUtil.getScreenSize(context).getHeight() * 0.7f));
            }
            // 别一上来就弹键盘，先让列表拿到焦点
            binding.actionSearch.clearFocus();
            binding.actionList.requestFocus();
        });
        dialog.show();
    }

    // 部分动作标题是带占位符的模板（如“%s 变量-%s”），展示时去掉占位符
    private static String displayTitle(ActionInfo info, ActionType type) {
        if (info == null) return type.name();
        String title = info.getTitle();
        if (title == null) return type.name();
        title = title.replace("%s", "").replace("%d", "").replaceAll("\\s+", " ").trim();
        return title.isEmpty() ? type.name() : title;
    }

    private static class Adapter extends RecyclerView.Adapter<Holder> {        private final List<ActionType> items = new ArrayList<>();
        private final Listener listener;

        Adapter(List<ActionType> source, Listener listener) {
            items.addAll(source);
            this.listener = listener;
        }

        void submit(List<ActionType> source) {
            items.clear();
            items.addAll(source);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemTaskCodeActionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        private final ItemTaskCodeActionBinding binding;

        Holder(ItemTaskCodeActionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ActionType type, Listener listener) {
            ActionInfo info = ActionInfo.getActionInfo(type);
            ImageView icon = binding.actionIcon;
            TextView title = binding.actionTitle;
            TextView description = binding.actionDesc;
            if (info == null) {
                icon.setImageDrawable(null);
                title.setText(type.name());
                description.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(null);
                return;
            }
            icon.setImageResource(info.getIcon());
            title.setText(displayTitle(info, type));
            String text = info.getDescription();
            description.setText(text == null ? "" : text);
            description.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> listener.onPick(type));
        }
    }
}
