package org.autojs.autoxjs.ui.edit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.stardust.autojs.script.JavaScriptSource;
import com.stardust.pio.PFiles;
import com.stardust.util.ClipboardUtil;
import com.stardust.util.IntentUtil;

import org.autojs.autoxjs.R;
import org.autojs.autoxjs.model.indices.AndroidClass;
import org.autojs.autoxjs.model.indices.ClassSearchingItem;
import org.autojs.autoxjs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autoxjs.ui.build.BuildActivity;
import org.autojs.autoxjs.ui.common.NotAskAgainDialog;
import org.autojs.autoxjs.ui.edit.editor.CodeEditor;
import org.autojs.autoxjs.ui.log.LogActivityKt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Stardust on 2017/9/28.
 */

@SuppressLint("CheckResult")
public class EditorMenu {

    private EditorView mEditorView;
    private Context mContext;
    private CodeEditor mEditor;

    public EditorMenu(EditorView editorView) {
        mEditorView = editorView;
        mContext = editorView.getContext();
        mEditor = editorView.getEditor();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log:
                showLog();
                return true;
            case R.id.action_force_stop:
                forceStop();
                return true;
            default:
                if (onEditOptionsSelected(item)) {
                    return true;
                }
                if (onJumpOptionsSelected(item)) {
                    return true;
                }
                if (onMoreOptionsSelected(item)) {
                    return true;
                }
                if (onDebugOptionsSelected(item)) {
                    return true;
                }
        }
        return false;
    }

    private boolean onDebugOptionsSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_breakpoint:
                mEditor.addOrRemoveBreakpointAtCurrentLine();
                return true;
            case R.id.action_launch_debugger:
                new NotAskAgainDialog.Builder(mEditorView.getContext(), "editor.debug.long_click_hint")
                        .title(R.string.text_alert)
                        .content(R.string.hint_long_click_run_to_debug)
                        .positiveText(R.string.ok)
                        .show();
                mEditorView.debug();
                return true;
            case R.id.action_remove_all_breakpoints:
                mEditor.removeAllBreakpoints();
                return true;
        }
        return false;
    }

    private boolean onJumpOptionsSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_jump_to_line:
                jumpToLine();
                return true;
            case R.id.action_jump_to_start:
                mEditor.jumpToStart();
                return true;
            case R.id.action_jump_to_end:
                mEditor.jumpToEnd();
                return true;
            case R.id.action_jump_to_line_start:
                mEditor.jumpToLineStart();
                return true;
            case R.id.action_jump_to_line_end:
                mEditor.jumpToLineEnd();
                return true;
        }
        return false;
    }


    private boolean onMoreOptionsSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_console:
                showConsole();
                return true;
            case R.id.action_import_java_class:
                importJavaPackageOrClass();
                return true;
            case R.id.action_editor_text_size:
                mEditorView.selectTextSize();
                return true;
            case R.id.action_editor_theme:
                mEditorView.selectEditorTheme();
                return true;
            case R.id.action_open_by_other_apps:
                openByOtherApps();
                return true;
            case R.id.action_info:
                showInfo();
                return true;

            case R.id.action_build_apk:
                startBuildApkActivity();
                return true;

        }
        return false;
    }

    private void importJavaPackageOrClass() {
        mEditor.getSelection()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s ->
                        new ClassSearchDialogBuilder(mContext)
                                .setQuery(s)
                                .itemClick((dialog, item, pos) -> showClassSearchingItem(dialog, item))
                                .title(R.string.text_search_java_class)
                                .show()
                );
    }

    private void showClassSearchingItem(MaterialDialog dialog, ClassSearchingItem item) {
        String title;
        String desc;
        if (item instanceof ClassSearchingItem.ClassItem) {
            AndroidClass androidClass = ((ClassSearchingItem.ClassItem) item).getAndroidClass();
            title = androidClass.getClassName();
            desc = androidClass.getFullName();
        } else {
            title = ((ClassSearchingItem.PackageItem) item).getPackageName();
            desc = title;
        }
        new ThemeColorMaterialDialogBuilder(mContext)
                .title(title)
                .content(desc)
                .positiveText(R.string.text_copy)
                .negativeText(R.string.text_en_import)
                .neutralText(R.string.text_view_docs)
                .onPositive((ignored, which) -> {
                    ClipboardUtil.setClip(mContext, desc);
                    Toast.makeText(mContext, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .onNegative((ignored, which) -> {
                    if (mEditor.getText().startsWith(JavaScriptSource.EXECUTION_MODE_UI_PREFIX)) {
                        mEditor.insert(1, item.getImportText() + ";\n");
                    } else {
                        mEditor.insert(0, item.getImportText() + ";\n");
                    }
                })
                .onNeutral((ignored, which) -> IntentUtil.browse(mContext, item.getUrl()))
                .onAny((ignored, which) -> dialog.dismiss())
                .show();
    }

    private void startBuildApkActivity() {
        BuildActivity.Companion.start(mContext, mEditorView.getUri().getPath());
    }


    private boolean onEditOptionsSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_as:
                showSaveAsDialog();
                return true;
            case R.id.action_paste:
                paste();
                return true;
            case R.id.action_copy_all:
                copyAll();
                return true;
            case R.id.action_copy_line:
                copyLine();
                return true;
            case R.id.action_delete_line:
                deleteLine();
                return true;
            case R.id.action_clear:
                mEditor.setText("");
                return true;
            case R.id.action_beautify:
                beautifyCode();
                return true;
            case R.id.action_find_or_replace:
                findOrReplace();
                return true;
        }
        return false;
    }

    private void jumpToLine() {
        mEditor.getLineCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showJumpDialog);
    }

    private void showJumpDialog(final int lineCount) {
        String hint = "1 ~ " + lineCount;
        new ThemeColorMaterialDialogBuilder(mContext)
                .title(R.string.text_jump_to_line)
                .input(hint, "", (dialog, input) -> {
                    if (TextUtils.isEmpty(input)) {
                        return;
                    }
                    try {
                        int line = Integer.parseInt(input.toString());
                        mEditor.jumpTo(line - 1, 0);
                    } catch (NumberFormatException ignored) {
                    }
                })
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .show();
    }

    private void showInfo() {
        Observable.zip(Observable.just(mEditor.getText()), mEditor.getLineCount(), (text, lineCount) -> {
                    String size = PFiles.getHumanReadableSize(text.length());
                    return String.format(Locale.getDefault(), mContext.getString(R.string.format_editor_info),
                            text.length(), lineCount, size);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showInfo);

    }

    private void showInfo(String info) {
        new ThemeColorMaterialDialogBuilder(mContext)
                .title(R.string.text_info)
                .content(info)
                .show();
    }

    private void copyLine() {
        mEditor.copyLine();
    }

    private void deleteLine() {
        mEditor.deleteLine();
    }

    private void paste() {
        mEditor.insert(ClipboardUtil.getClip(mContext).toString());
    }

    private void showSaveAsDialog() {

        // 创建一个 AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("保存路径");

        // 创建一个输入框
        final EditText input = new EditText(mContext);
        input.setText(mEditorView.getUri().getPath());
        builder.setView(input);

        // 设置确认按钮
        builder.setPositiveButton("确认", (dialog, which) -> {
            // 获取输入框的内容
            String inputText = input.getText().toString();
            // 在这里处理输入的内容
            File newFile = new File(inputText);
            if (newFile.isDirectory()) {
                Toast.makeText(mContext, "路径无效", Toast.LENGTH_SHORT).show();
            } else {
                if (newFile.exists()) {
                    AlertDialog.Builder overWriteBuilder = new AlertDialog.Builder(mContext);
                    overWriteBuilder.setTitle("文件已存在, 确认覆盖?");
                    overWriteBuilder.setPositiveButton("确认", (overWriteDialog, overWriteWhich) -> {
                        try {
                            FileWriter fileWriter = new FileWriter(newFile);
                            fileWriter.write(mEditor.getText());
                            fileWriter.close();
                            Toast.makeText(mContext, "覆盖文件: " + inputText, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    // 设置取消按钮
                    overWriteBuilder.setNegativeButton("取消", (overWriteDialog, overWriteWhich) -> {
                        overWriteDialog.cancel();
                        Toast.makeText(mContext, "取消保存", Toast.LENGTH_SHORT).show();
                    });
                    overWriteBuilder.show();
                } else {
                    try {
                        newFile.createNewFile();
                        if (newFile.exists()) {
                            FileWriter fileWriter = new FileWriter(newFile);
                            fileWriter.write(mEditor.getText());
                            fileWriter.close();
                            Toast.makeText(mContext, "另存为: " + inputText, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "创建文件失败\n请检查文件名称", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(mContext, "取消保存", Toast.LENGTH_SHORT).show();
        });

        // 显示对话框
        builder.show();

    }

    private void findOrReplace() {
        mEditor.getSelection()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s ->
                        new FindOrReplaceDialogBuilder(mContext, mEditorView)
                                .setQueryIfNotEmpty(s)
                                .show()
                );

    }

    private void copyAll() {
        ClipboardUtil.setClip(mContext, mEditor.getText());
        Snackbar.make(mEditorView, R.string.text_already_copy_to_clip, Snackbar.LENGTH_SHORT).show();
    }


    private void showLog() {
        LogActivityKt.start(mContext);
    }

    private void showConsole() {
        mEditorView.showConsole();
    }

    private void forceStop() {
        mEditorView.forceStop();
    }

    private void openByOtherApps() {
        mEditorView.openByOtherApps();
    }

    private void beautifyCode() {
        mEditorView.beautifyCode();
    }

}
