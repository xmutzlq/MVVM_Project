package com.bop.tiebei;

/**
 * @author lq.zeng
 * @date 2018/8/6
 */

public class MyViewTag {

    public static final int TYPE_EDIT = 0; //编辑框
    public static final int TYPE_TEXT = 1; //文本框
    public static final int TYPE_CHOICE = 2; //选择类型

    public String value;
    public String tag;
    public int type = TYPE_EDIT;
    public boolean isTime = false;

    public MyViewTag(String tag) {
        this.tag = tag;
    }

    public MyViewTag(String tag, int type, String value) {
        this.tag = tag;
        this.type = type;
        this.value = value;
    }

    public MyViewTag(String tag, int type, String value, boolean isTime) {
        this.tag = tag;
        this.type = type;
        this.value = value;
        this.isTime = isTime;
    }
}
