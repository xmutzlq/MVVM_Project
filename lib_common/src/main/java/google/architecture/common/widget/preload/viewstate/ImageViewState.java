package google.architecture.common.widget.preload.viewstate;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * @author lq.zeng
 * @date 2018/7/9
 */

public class ImageViewState extends ViewState<ImageView> {
    Drawable source;

    public ImageViewState(ImageView imageView) {
        super(imageView);
    }

    @Override
    public void beforeStart() {
        super.beforeStart();
        this.source = view.getDrawable();
        view.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    protected void restore() {
        this.view.setImageDrawable(source);
    }
}
