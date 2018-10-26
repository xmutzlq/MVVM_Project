package google.architecture.common.widget.address;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import google.architecture.common.R;


/**
 * @author lq.zeng
 * @date 2018/5/29
 */

public class SelectAdapter extends BaseAdapter {
    List<ISelectAble> datas;
    int selectedIndex = Selector.INDEX_INVALID;
    public SelectAdapter(List<ISelectAble> datas) {
        this.datas = datas;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int position) {
        return datas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return datas.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_area, parent, false);

            holder = new Holder();
            holder.textView = (TextView) convertView.findViewById(R.id.textView);
            holder.imageViewCheckMark = (ImageView) convertView.findViewById(R.id.imageViewCheckMark);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        ISelectAble item = (ISelectAble) getItem(position);
        holder.textView.setText(item.getName());

        boolean checked = selectedIndex != Selector.INDEX_INVALID && datas.get(selectedIndex).getId() == item.getId();
        holder.textView.setEnabled(!checked);
        holder.imageViewCheckMark.setVisibility(checked ? View.VISIBLE : View.GONE);

        return convertView;
    }
    class Holder {
        TextView textView;
        ImageView imageViewCheckMark;
    }
}
