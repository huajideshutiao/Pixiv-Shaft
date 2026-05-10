package ceui.lisa.adapters;


import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public class ViewHolder<BindView extends ViewBinding> extends RecyclerView.ViewHolder {

    public BindView baseBind;

    public ViewHolder(BindView pBaseBind) {
        super(pBaseBind.getRoot());
        baseBind = pBaseBind;
    }
}
