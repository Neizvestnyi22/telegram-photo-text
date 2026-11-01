package org.telegram.lab.phototext;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.lab.TouchDriver;
import org.telegram.lab.ui.Bubble;
import org.telegram.lab.ui.Bulletin;
import org.telegram.lab.ui.ChatHeaderView;
import org.telegram.lab.ui.ChatScreen;
import org.telegram.lab.ui.MessageCell;
import org.telegram.lab.ui.WallpaperDrawable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.PhotoText;
import org.telegram.ui.Components.PhotoTextOverlay;

import java.util.ArrayList;

public class MainActivity extends LabActivity {

    private ChatScreen chat;
    private Bitmap photo;
    private PhotoCell photoCell;
    private FrameLayout viewer;
    private ImageView image;
    private PhotoTextOverlay overlay;
    private LinearLayout actions;
    private LinearLayout chips;
    private TextView textButton, copyButton;
    private final PhotoText recognizer = new PhotoText();
    private final ArrayList<View> chipViews = new ArrayList<>();
    private ArrayList<PhotoText.Line> selection = new ArrayList<>();

    @Override
    protected void onCreateScreen(FrameLayout content) {
        photo = BitmapFactory.decodeResource(getResources(), org.telegram.lab.phototext.R.drawable.cafe_note);
        chat = new ChatScreen(this, WallpaperDrawable.GREEN);
        content.addView(chat);
        chat.header.setTitle("Emma Torres").setSubtitle("online", true).setAvatar("Emma Torres");
        chat.header.addAction(R.drawable.tg_ic_call);
        chat.header.addAction(R.drawable.tg_ic_ab_other);
        chat.addDate("Today");
        chat.add(new MessageCell(this, true).setText("Where are you? 🙂").setTime("12:38").setStatus(MessageCell.STATUS_READ));
        chat.add(new MessageCell(this, false).setText("At Café North, come over! Wi-Fi is on the note").setTime("12:40").setTail(false));
        photoCell = chat.add(new PhotoCell());
        chat.scrollToBottom();
        photoCell.setOnClickListener(v -> openViewer());
        // Warm the recognizer up once, so the first long press answers right away.
        recognizer.recognize(Bitmap.createScaledBitmap(photo, 120, 150, true), (l, e) -> {
        });
        buildViewer(content);
    }

    /** Photo message: rounded picture in an incoming bubble. */
    private class PhotoCell extends View {
        final RectF bubble = new RectF();
        final RectF pic = new RectF();
        private final Path clip = new Path();

        PhotoCell() {
            super(MainActivity.this);
        }

        @Override
        protected void onMeasure(int w, int h) {
            int pw = AndroidUtilities.dp(250);
            setMeasuredDimension(MeasureSpec.getSize(w), (int) (pw * photo.getHeight() / (float) photo.getWidth()) + AndroidUtilities.dp(10));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float l = AndroidUtilities.dp(10);
            bubble.set(l, AndroidUtilities.dp(3), l + AndroidUtilities.dp(256), getHeight() - AndroidUtilities.dp(3));
            Bubble.draw(canvas, bubble, false, true);
            pic.set(bubble.left + AndroidUtilities.dp(3), bubble.top + AndroidUtilities.dp(3), bubble.right - AndroidUtilities.dp(3), bubble.bottom - AndroidUtilities.dp(3));
            clip.reset();
            clip.addRoundRect(pic, AndroidUtilities.dp(14), AndroidUtilities.dp(14), Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            canvas.drawBitmap(photo, null, pic, null);
            canvas.restore();
        }
    }

    private void buildViewer(FrameLayout content) {
        viewer = new FrameLayout(this);
        viewer.setBackgroundColor(0xFF000000);
        viewer.setVisibility(View.GONE);
        viewer.setElevation(AndroidUtilities.dp(30));
        content.addView(viewer, new FrameLayout.LayoutParams(-1, -1));

        image = new ImageView(this);
        image.setImageBitmap(photo);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setOnLongClickListener(v -> {
            recognize();
            return true;
        });
        viewer.addView(image, new FrameLayout.LayoutParams(-1, -1));

        overlay = new PhotoTextOverlay(this);
        overlay.setVisibility(View.GONE);
        overlay.setListener(sel -> {
            selection = sel;
            copyButton.setText(sel.isEmpty() ? "Copy all" : "Copy " + sel.size() + (sel.size() == 1 ? " line" : " lines"));
        });
        viewer.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x99000000, 0x00000000}));
        top.addView(LabActivity.statusBar(this, true));
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(this);
        back.setImageDrawable(Tg.icon(this, R.drawable.tg_ic_ab_back, 0xFFFFFFFF));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setOnClickListener(v -> closeViewer());
        bar.addView(back, new LinearLayout.LayoutParams(AndroidUtilities.dp(52), AndroidUtilities.dp(52)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView name = Tg.text(this, 17, 0xFFFFFFFF, true);
        name.setText("Emma Torres");
        TextView date = Tg.text(this, 13.5f, 0xB3FFFFFF, false);
        date.setText("today at 12:40");
        titles.addView(name);
        titles.addView(date);
        bar.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        ImageView more = new ImageView(this);
        more.setImageDrawable(Tg.icon(this, R.drawable.tg_ic_ab_other, 0xFFFFFFFF));
        more.setScaleType(ImageView.ScaleType.CENTER);
        bar.addView(more, new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(52)));
        top.addView(bar);
        viewer.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(14));
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x99000000, 0x00000000}));
        for (int icon : new int[]{R.drawable.tg_msg_share, R.drawable.tg_msg_forward}) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(Tg.icon(this, icon, 0xFFFFFFFF));
            iv.setScaleType(ImageView.ScaleType.CENTER);
            bottom.addView(iv, new LinearLayout.LayoutParams(AndroidUtilities.dp(52), AndroidUtilities.dp(48)));
        }
        bottom.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        textButton = Tg.text(this, 14.5f, 0xFFFFFFFF, true);
        textButton.setText("Text");
        textButton.setCompoundDrawablesRelativeWithIntrinsicBounds(Tg.icon(this, R.drawable.tg_msg_text_outlined, 0xFFFFFFFF), null, null, null);
        textButton.setCompoundDrawablePadding(AndroidUtilities.dp(6));
        textButton.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(0x33FFFFFF);
        tb.setCornerRadius(AndroidUtilities.dp(20));
        textButton.setBackground(tb);
        textButton.setOnClickListener(v -> {
            if (overlay.isShown2()) {
                hideText();
            } else {
                recognize();
            }
        });
        bottom.addView(textButton);
        viewer.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setBackground(ChatHeaderView.glass(AndroidUtilities.dp(22)));
        actions.setElevation(AndroidUtilities.dp(8));
        actions.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(6));
        TextView found = Tg.text(this, 13, Tg.textGray, true);
        found.setText("FOUND ON THE PHOTO");
        found.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        actions.addView(found);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chips = new LinearLayout(this);
        chips.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        hs.addView(chips);
        actions.addView(hs);
        View div = new View(this);
        div.setBackgroundColor(Tg.divider);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(1));
        dl.topMargin = AndroidUtilities.dp(10);
        actions.addView(div, dl);
        LinearLayout tools = new LinearLayout(this);
        copyButton = tool("Copy all", R.drawable.tg_msg_copy, v -> copySelection());
        tools.addView(centered(copyButton), new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1f));
        tools.addView(centered(tool("Select all", R.drawable.tg_msg_select, v -> overlay.selectAll())), new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1f));
        tools.addView(centered(tool("Translate", R.drawable.tg_msg_translate, v -> bulletin(R.drawable.tg_msg_translate, "Translation opens in Telegram's translate sheet").show(2000))), new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1f));
        actions.addView(tools);
        FrameLayout.LayoutParams al = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        al.leftMargin = al.rightMargin = AndroidUtilities.dp(10);
        al.bottomMargin = AndroidUtilities.dp(80);
        viewer.addView(actions, al);
        actions.setVisibility(View.GONE);
    }

    /** Keeps an icon+label button compact and centered in its third of the row. */
    private View centered(View v) {
        FrameLayout f = new FrameLayout(this);
        f.addView(v, new FrameLayout.LayoutParams(-2, -1, Gravity.CENTER));
        return f;
    }

    private TextView tool(String label, int icon, View.OnClickListener l) {
        TextView t = Tg.text(this, 14, Tg.accent, true);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setCompoundDrawablesRelativeWithIntrinsicBounds(Tg.icon(this, icon, Tg.accent), null, null, null);
        t.setCompoundDrawablePadding(AndroidUtilities.dp(6));
        t.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        t.setOnClickListener(l);
        return t;
    }

    private void openViewer() {
        viewer.setVisibility(View.VISIBLE);
        viewer.setAlpha(0);
        image.setScaleX(0.6f);
        image.setScaleY(0.6f);
        viewer.animate().alpha(1).setDuration(220).start();
        image.animate().scaleX(1).scaleY(1).setDuration(300).start();
    }

    private void closeViewer() {
        hideText();
        viewer.animate().alpha(0).setDuration(200).withEndAction(() -> viewer.setVisibility(View.GONE)).start();
    }

    private void recognize() {
        textButton.setAlpha(0.5f);
        recognizer.recognize(photo, (lines, entities) -> {
            textButton.setAlpha(1f);
            overlay.setImageMatrix(image.getImageMatrix());
            overlay.show(lines);
            selection = new ArrayList<>();
            copyButton.setText("Copy all");
            fillChips(entities);
            actions.setVisibility(View.VISIBLE);
            actions.setAlpha(0);
            actions.setTranslationY(AndroidUtilities.dp(30));
            actions.animate().alpha(1).translationY(0).setStartDelay(350).setDuration(260).start();
            GradientDrawable tb = (GradientDrawable) textButton.getBackground();
            tb.setColor(Tg.accent);
        });
    }

    private void hideText() {
        overlay.hide();
        actions.setVisibility(View.GONE);
        ((GradientDrawable) textButton.getBackground()).setColor(0x33FFFFFF);
    }

    private void fillChips(ArrayList<PhotoText.Entity> entities) {
        chips.removeAllViews();
        chipViews.clear();
        for (PhotoText.Entity e : entities) {
            int icon;
            String label, done;
            switch (e.type) {
                case PhotoText.PASSWORD:
                    icon = R.drawable.tg_msg_permissions;
                    label = "Copy password";
                    done = "Wi-Fi password copied: " + e.value;
                    break;
                case PhotoText.LINK:
                    icon = R.drawable.tg_msg_link;
                    label = e.value;
                    done = "Opening " + e.value;
                    break;
                case PhotoText.PHONE:
                    icon = R.drawable.tg_msg_calls;
                    label = e.value;
                    done = "Calling " + e.value;
                    break;
                default:
                    icon = R.drawable.tg_msg_message;
                    label = e.value;
                    done = "New e-mail to " + e.value;
                    break;
            }
            TextView c = Tg.text(this, 14, Tg.text, true);
            c.setText(label);
            c.setCompoundDrawablesRelativeWithIntrinsicBounds(Tg.icon(this, icon, Tg.accent), null, null, null);
            c.setCompoundDrawablePadding(AndroidUtilities.dp(6));
            c.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(14), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF0F2F5);
            bg.setCornerRadius(AndroidUtilities.dp(18));
            c.setBackground(bg);
            c.setOnClickListener(v -> {
                if (e.type == PhotoText.PASSWORD) {
                    clip(e.value);
                }
                bulletin(icon, done).show(2200);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, AndroidUtilities.dp(36));
            lp.rightMargin = AndroidUtilities.dp(8);
            chips.addView(c, lp);
            chipViews.add(c);
        }
    }

    private void copySelection() {
        StringBuilder sb = new StringBuilder();
        ArrayList<PhotoText.Line> list = selection;
        if (list.isEmpty()) {
            overlay.selectAll();
            list = selection;
        }
        for (PhotoText.Line l : list) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(l.text);
        }
        clip(sb.toString());
        bulletin(R.drawable.tg_msg_copy, list.size() == 1 ? "Copied: " + sb : list.size() + " lines copied").show(2200);
    }

    /** Notices in the viewer appear at the top so they never cover the action card. */
    private Bulletin bulletin(int icon, String text) {
        Bulletin b = Bulletin.make(viewer, icon, text);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) b.getLayoutParams();
        lp.gravity = Gravity.TOP;
        lp.bottomMargin = 0;
        lp.topMargin = LabActivity.statusBarHeight + AndroidUtilities.dp(60);
        b.setLayoutParams(lp);
        b.setElevation(AndroidUtilities.dp(12));
        return b;
    }

    private void clip(String s) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text", s));
    }

    @Override
    protected void onAutoplay(TouchDriver d) {
        d.wait(900)
                .tap(photoCell)
                .wait(1200)
                .longPress(image, 650)
                .waitFor(() -> !chipViews.isEmpty(), 15000)
                .wait(1200)
                .zoom(1.4f, () -> actions, 900)
                .wait(800)
                .tap(() -> chipViews.get(0))
                .wait(2300)
                .resetZoom(800)
                .tapPoint(() -> {
                    int i = Math.max(0, overlay.find("Open daily"));
                    RectF r = overlay.lineRect(i);
                    return new float[]{r.centerX(), r.centerY()};
                })
                .wait(900)
                .zoom(1.4f, () -> actions, 800)
                .tap(() -> copyButton)
                .wait(2200)
                .tap(() -> chipViews.size() > 1 ? chipViews.get(1) : copyButton)
                .wait(2200)
                .resetZoom(900)
                .wait(1000);
    }
}
