package google.architecture.common.base.listener;

import android.view.View;

public interface OnItemClickListener<T> {

    void onItemClick(View view, int position, T bean);
}
