package ceui.lisa.feature.worker;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ceui.lisa.activities.Shaft;

import static ceui.lisa.core.CallExtKt.executeCall;
import ceui.lisa.http.ErrorCtrl;
import ceui.lisa.http.Retro;
import ceui.lisa.models.NullResponse;
import ceui.lisa.utils.Params;

import java.util.function.Function;

public class BatchStarTask extends AbstractTask {

    private final int illustID;
    private final int starType; // 0收藏，1取消收藏

    public BatchStarTask(String name, int illustID, int starType) {
        this.illustID = illustID;
        this.starType = starType;
        if (starType == 0) {
            this.name = ("添加收藏 " + name);
        } else {
            this.name = ("取消收藏 " + name);
        }
    }

    @Override
    public void run(IEnd end) {
        if (starType == 0) {
            executeCall(
                Retro.getAppApi().postLikeIllust(illustID, Params.TYPE_PUBLIC),
                Function.identity(),
                new ErrorCtrl<NullResponse>() {
                        @Override
                        public void next(NullResponse nullResponse) {
                            Intent intent = new Intent(Params.LIKED_ILLUST);
                            intent.putExtra(Params.ID, illustID);
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
                Retro.getAppApi().postDislikeIllust(illustID),
                Function.identity(),
                new ErrorCtrl<NullResponse>() {
                        @Override
                        public void next(NullResponse nullResponse) {
                            Intent intent = new Intent(Params.LIKED_ILLUST);
                            intent.putExtra(Params.ID, illustID);
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
