package org.autojs.autoxjs.ui.main.task;

import static org.autojs.autoxjs.ui.timing.TimedTaskSettingActivity.ACTION_DESC_MAP;
import static java.lang.Thread.sleep;

import android.util.Log;

import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.engine.ScriptEngine;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.AutoFileSource;
import com.stardust.autojs.script.JavaScriptSource;
import com.stardust.autojs.script.ScriptSource;
import com.stardust.autojs.script.StringScriptSource;
import com.stardust.pio.PFiles;

import org.autojs.autoxjs.R;
import org.autojs.autoxjs.timing.IntentTask;
import org.autojs.autoxjs.timing.TimedTask;
import org.autojs.autoxjs.timing.TimedTaskManager;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;

/**
 * Created by Stardust on 2017/11/28.
 */

public abstract class Task {


    public abstract String getName();

    public abstract String getDesc();

    public abstract void cancel();

    public abstract String getEngineName();

    public static class PendingTask extends Task {


        private TimedTask mTimedTask;
        private IntentTask mIntentTask;


        public PendingTask(TimedTask timedTask) {
            mTimedTask = timedTask;
            mIntentTask = null;
        }

        public PendingTask(IntentTask intentTask) {
            mIntentTask = intentTask;
            mTimedTask = null;
        }

        public boolean taskEquals(Object task) {
            if (mTimedTask != null) {
                return mTimedTask.equals(task);
            }
            return mIntentTask.equals(task);
        }

        public TimedTask getTimedTask() {
            return mTimedTask;
        }

        @Override
        public String getName() {
            return PFiles.getSimplifiedPath(getScriptPath());
        }

        @Override
        public String getDesc() {
            if (mTimedTask != null) {
                long nextTime = mTimedTask.getNextTime();
                return GlobalAppContext.getString(R.string.text_next_run_time) + ": " +
                        DateTimeFormat.forPattern("yyyy/MM/dd HH:mm").print(nextTime);
            } else {
                assert mIntentTask != null;
                Integer desc = ACTION_DESC_MAP.get(mIntentTask.getAction());
                if(desc != null){
                    return GlobalAppContext.getString(desc);
                }
                return mIntentTask.getAction();
            }

        }

        @Override
        public void cancel() {
            if (mTimedTask != null) {
                TimedTaskManager.INSTANCE.removeTask(mTimedTask);
            } else {
                TimedTaskManager.INSTANCE.removeTask(mIntentTask);
            }
        }

        private String getScriptPath() {
            if (mTimedTask != null) {
                return mTimedTask.getScriptPath();
            } else {
                assert mIntentTask != null;
                return mIntentTask.getScriptPath();
            }
        }

        @Override
        public String getEngineName() {
            if (getScriptPath().endsWith(".js")) {
                return JavaScriptSource.ENGINE;
            } else {
                return AutoFileSource.ENGINE;
            }
        }

        public void setTimedTask(TimedTask timedTask) {
            mTimedTask = timedTask;
        }

        public void setIntentTask(IntentTask intentTask) {
            mIntentTask = intentTask;
        }

        public long getId() {
            if(mTimedTask != null)
                return mTimedTask.getId();
            return mIntentTask.getId();
        }
    }

    public static class RunningTask extends Task {
        private final ScriptExecution mScriptExecution;

        public RunningTask(ScriptExecution scriptExecution) {
            mScriptExecution = scriptExecution;
        }

        public ScriptExecution getScriptExecution() {
            return mScriptExecution;
        }

        @Override
        public String getName() {
            return mScriptExecution.getSource().getName();
        }

        @Override
        public String getDesc() {
            return mScriptExecution.getSource().toString();
        }

        @Override
        public void cancel() {
            ScriptEngine engine = mScriptExecution.getEngine();
            ScriptSource exit = new StringScriptSource(new Date().getTime()+"", "threads.start(()=>{exit()})");
            if (engine != null) {
                try{
                    engine.forceStop();
                    engine.execute(exit);
                }catch(Exception e){
                    Log.d("ozobiLog","Task: cancel: e: "+e);
                }
            }
        }

        @Override
        public String getEngineName() {
            return mScriptExecution.getSource().getEngineName();
        }
    }
}
