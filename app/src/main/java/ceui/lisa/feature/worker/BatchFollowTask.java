package ceui.lisa.feature.worker;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ceui.lisa.activities.Shaft;

import static ceui.lisa.core.CallExtKt.executeCall;
import ceui.lisa.http.ErrorCtrl;
import ceui.lisa.http.Retro;
import ceui.lisa.models.NullResponse;
import ceui.lisa.utils.Params;
import ceui.pixiv.widgets.RateAppManager;

import java.util.function.Function;

public class BatchFollowTask extends AbstractTask {

    private final int userID;
    private final int starType; // 0关注，1取消关注

    public BatchFollowTask(String name, int userID, int starType) {
        this.starType = starType;
        this.userID = userID;
        if (starType == 0) {
            this.name = ("添加关注 " + name);
        } else {
            this.name = ("取消关注 " + name);
        }
    }

    @Override
    public void run(IEnd end) {
        if (starType == 0) {
            executeCall(
                Retro.getAppApi().postFollow(
                    userID, Params.TYPE_PUBLIC),
                Function.identity(),
                new ErrorCtrl<NullResponse>() {
                        @Override
                        public void next(NullResponse nullResponse) {
                            RateAppManager.INSTANCE.onUserEngaged();
                            Intent intent = new Intent(Params.LIKED_USER);
                            intent.putExtra(Params.ID, userID);
                            intent.putExtra(Params.IS_LIKED, true);
                            LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
                        }

                        @Override
                        public void must() {
                            end.next();
                        }
                    });
        } else {
            executeCall(
                Retro.getAppApi().postUnFollow(
                    userID), Function.identity(), new ErrorCtrl<NullResponse>() {
                        @Override
                        public void next(NullResponse nullResponse) {
                            Intent intent = new Intent(Params.LIKED_USER);
                            intent.putExtra(Params.ID, userID);
                            intent.putExtra(Params.IS_LIKED, false);
                            LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
                        }

                        @Override
                        public void must() {
                            end.next();
                        }
                    });
        }
    }
}
