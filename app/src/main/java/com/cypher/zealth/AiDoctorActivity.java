package com.cypher.zealth;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.TextKeyListener;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiDoctorActivity extends AppCompatActivity {

    private static final String TAG = "AiDoctor";
    private static final int CHIP_BG_TEAL = 0xFF0EA5A4;
    private static final int CHIP_BG_TEAL_ALT = 0xFF14B8A6;
    private static final int CHIP_TEXT_WHITE = 0xFFFFFFFF;
    private static final int CHIP_STROKE = 0x22000000;
    private static final int CHIP_RIPPLE = 0x14000000;
    private static final int LINK_TEAL = 0xFF0EA5A4;
    private static final int MAX_CONTEXT_TURNS = 10;
    private static final int MIN_REPEAT_SIMILARITY_PERCENT = 90;
    private RecyclerView recycler;
    private TextInputEditText input;
    private MaterialButton btnSend;
    private ImageButton btnBack;
    private MaterialButton btnEmergency;
    private ChipGroup quickChips;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService aiExec = Executors.newSingleThreadExecutor();
    private OpenAIClient apiClient;
    private boolean awaitingResponse = false;
    private String lastBotNormalized = null;
    private final Set<String> userReplyMemory = new HashSet<>();
    private boolean lastImeVisible = false;

    private static final List<String> SUGGESTIONS = Arrays.asList(
            "Sleep routine tips",
            "Stress management techniques",
            "Nutrition basics",
            "Healthy weight goals",
            "Activity and fitness planning",
            "Understanding common cold vs flu (general)",
            "When to consider seeing a clinician (general)"
    );



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        setContentView(R.layout.activity_ai_doctor);

        apiClient = new OpenAIClient();

        btnBack = findViewById(R.id.btnBack);
        btnEmergency = findViewById(R.id.btnEmergency);

        btnBack.setOnClickListener(v -> finish());
        btnEmergency.setOnClickListener(v -> showEmergencyDialog());

        recycler = findViewById(R.id.recyclerChat);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setItemAnimator(null);

        recycler.setFocusable(false);
        recycler.setFocusableInTouchMode(false);
        recycler.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        adapter = new ChatAdapter(messages, new ChatAdapter.Callbacks() {
            @Override public void onQuickReply(String text) { setInput(text, true); }
            @Override public void onRetryLast() {
                String lastUser = findLastUserMessage();
                if (!TextUtils.isEmpty(lastUser)) {
                    setInput(lastUser, true);
                    sendUserMessage();
                }
            }
            @Override public void onToggleBotExpanded(int position) {
                if (position < 0 || position >= messages.size()) return;
                ChatMessage m = messages.get(position);
                if (m.type != ChatMessage.TYPE_BOT) return;
                m.expanded = !m.expanded;
                adapter.notifyItemChanged(position);
            }
        });
        recycler.setAdapter(adapter);

        input = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        forceInputEditable();

        input.setOnClickListener(v -> focusAndShowKeyboard());
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                focusAndShowKeyboard();
                scrollToBottom();
            }
        });

        setupComposerBehavior();

        quickChips = findViewById(R.id.quickChips);
        populateSuggestionChips();

        applyWindowInsetsImeAndSystemBars();

        addBotMessage(
                "Hi — I can share general health information for education. I can’t diagnose, recommend treatments, or assess urgency. " +
                        "If you think this may be an emergency, call your local emergency number.\n\n" +
                        "What topic would you like information about (e.g., headache, sleep, stress, nutrition, exercise)?"
        );

    }

    private void forceInputEditable() {
        if (input == null) return;

        input.setEnabled(true);
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.setCursorVisible(true);
        input.setClickable(true);
        input.setLongClickable(true);

        if (input.getKeyListener() == null) {
            input.setKeyListener(TextKeyListener.getInstance());
        }


        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
    }

    private void focusAndShowKeyboard() {
        if (input == null) return;
        forceInputEditable();

        input.requestFocus();
        input.post(() -> {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            } catch (Throwable ignore) {}
        });
    }


    private void applyWindowInsetsImeAndSystemBars() {
        final View root = findViewById(R.id.rootFrame);
        final View bottomCard = findViewById(R.id.bottomCard);
        final View toolbarContainer = findViewById(R.id.toolbarContainer);

        if (root == null) return;

        final int baseBottomMargin = dp(12);
        final int baseToolbarTopPadding = (toolbarContainer != null) ? toolbarContainer.getPaddingTop() : 0;

        final int baseRecyclerBottomPad = (recycler != null) ? recycler.getPaddingBottom() : dp(12);
        final int baseRecyclerTopPad = (recycler != null) ? recycler.getPaddingTop() : dp(12);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(sys.bottom, ime.bottom);


            if (bottomCard != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomCard.getLayoutParams();
                lp.bottomMargin = baseBottomMargin + bottomInset;
                bottomCard.setLayoutParams(lp);
            }


            if (toolbarContainer != null) {
                toolbarContainer.setPadding(
                        toolbarContainer.getPaddingLeft(),
                        baseToolbarTopPadding + sys.top,
                        toolbarContainer.getPaddingRight(),
                        toolbarContainer.getPaddingBottom()
                );
            }

            if (recycler != null) {
                recycler.setPadding(
                        recycler.getPaddingLeft(),
                        baseRecyclerTopPad,
                        recycler.getPaddingRight(),
                        baseRecyclerBottomPad + bottomInset
                );
            }

            if (imeVisible && !lastImeVisible) {
                scrollToBottom();
            }
            lastImeVisible = imeVisible;

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void showEmergencyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Emergency help")
                .setMessage("If you believe you may be experiencing a medical emergency, contact local emergency services now.")
                .setPositiveButton("Open phone dialer", (d, w) -> {
                    try { startActivity(new Intent(Intent.ACTION_DIAL)); }
                    catch (Throwable t) { Toast.makeText(this, "Unable to open dialer.", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private void dialNumber(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
        } catch (Throwable t) {
            Toast.makeText(this, "Unable to open dialer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateSuggestionChips() {
        if (quickChips == null) return;
        quickChips.removeAllViews();

        for (String text : SUGGESTIONS) {
            Chip chip = buildDockSuggestionChip(text);
            chip.setOnClickListener(v -> setInput(text, true)); // fill input + focus + keyboard
            quickChips.addView(chip);
        }
    }

    private Chip buildDockSuggestionChip(String text) {
        Chip c = new Chip(this);
        c.setText(text);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        c.setEnsureMinTouchTargetSize(true);

        c.setClickable(true);
        c.setFocusable(true);
        c.setCheckable(false);
        c.setCheckedIconVisible(false);
        c.setCloseIconVisible(false);
        c.setChipIconVisible(false);

        c.setChipBackgroundColor(ColorStateList.valueOf(CHIP_BG_TEAL));
        c.setTextColor(CHIP_TEXT_WHITE);
        c.setRippleColor(ColorStateList.valueOf(CHIP_RIPPLE));

        c.setChipStrokeColor(ColorStateList.valueOf(CHIP_STROKE));
        c.setChipStrokeWidth(dp(1));
        c.setChipCornerRadius(dp(999));
        c.setMinHeight(dp(36));

        c.setPadding(dp(10), dp(2), dp(10), dp(2));
        return c;
    }

    private void setupComposerBehavior() {
        setSendEnabled(false, false);

        btnSend.setOnClickListener(v -> sendUserMessage());

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserMessage();
                return true;
            }
            return false;
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean enable = s != null && s.toString().trim().length() > 0 && !awaitingResponse;
                setSendEnabled(enable, true);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setComposerEnabled(boolean enabled) {
        // Keep input truly editable when re-enabled
        input.setEnabled(enabled);
        if (enabled) forceInputEditable();

        btnSend.setEnabled(enabled && input.getText() != null && input.getText().toString().trim().length() > 0);
        btnSend.setAlpha(enabled ? 1f : 0.45f);

        if (quickChips != null) quickChips.setEnabled(enabled);

        if (enabled) {
            
            focusAndShowKeyboard();
        }
    }

    private void setSendEnabled(boolean enabled, boolean animate) {
        btnSend.setEnabled(enabled && !awaitingResponse);
        btnSend.setAlpha((enabled && !awaitingResponse) ? 1f : 0.45f);

        if (animate) {
            float from = enabled ? 0.90f : 1.00f;
            float to = enabled ? 1.00f : 0.92f;
            btnSend.setScaleX(from);
            btnSend.setScaleY(from);
            btnSend.animate()
                    .scaleX(to)
                    .scaleY(to)
                    .setDuration(140)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            btnSend.setScaleX(enabled ? 1f : 0.92f);
            btnSend.setScaleY(enabled ? 1f : 0.92f);
        }
    }

    private void setInput(String text, boolean focusKeyboard) {
        input.setText(text);
        if (input.getText() != null) input.setSelection(input.getText().length());
        if (focusKeyboard) focusAndShowKeyboard();
    }

    private void sendUserMessage() {
        if (awaitingResponse) return;

        String text = input.getText() != null ? input.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        userReplyMemory.add(normalizeUserMemoryKey(text));

        addUserMessage(text);
        input.setText("");

        awaitingResponse = true;
        setComposerEnabled(false);

        int thinkingIndex = addBotMessageReturnIndex("…");

        final List<ChatMessage> snapshot = new ArrayList<>(messages);
        final String promptForApi = buildContextPrompt(text, snapshot);

        aiExec.submit(() -> {
            String reply;
            boolean connectivity = false;

            try {
               
                String local = AiDoctorEngine.respond(text, snapshot);

                boolean isEmergencyStyle =
                        local != null && (
                                local.toLowerCase(Locale.US).contains("this could be an emergency") ||
                                        local.toLowerCase(Locale.US).contains("call your local") ||
                                        local.toLowerCase(Locale.US).contains("seek urgent")
                        );

                if (isEmergencyStyle) {
                    reply = local;
                } else {
                  
                    reply = apiClient.chat(promptForApi);
                    if (reply == null || reply.trim().isEmpty()) reply = local;
                }

            } catch (Throwable t) {
                Log.e(TAG, "Chat crashed", t);

                String msg = t.getMessage() == null ? "" : t.getMessage();
                if (t instanceof UnknownHostException || msg.toLowerCase(Locale.US).contains("unable to resolve host")) {
                    connectivity = true;
                }

                reply = (connectivity) ? "__CONNECTIVITY__" : "AI error: " + msg;
            }

            final String finalReplyRaw = reply == null ? "" : reply.trim();
            final boolean finalConnectivity = connectivity;

            mainHandler.post(() -> {
                awaitingResponse = false;
                setComposerEnabled(true);

                if ("__CONNECTIVITY__".equals(finalReplyRaw) || finalConnectivity) {
                    replaceWithSystemMessage(thinkingIndex,
                            "Connection problem. Check internet/DNS and try again.",
                            true
                    );
                    return;
                }

                String finalReply = postProcessBotReply(finalReplyRaw, snapshot);
                replaceBotMessage(thinkingIndex, finalReply);

                lastBotNormalized = normalizeForRepeat(finalReply);

                List<String> qr = QuickReplies.suggest(finalReply, snapshot, userReplyMemory);
                if (!qr.isEmpty()) addBotQuickReplies(qr);
            });
        });
    }

    private String buildContextPrompt(String newUserText, List<ChatMessage> conversation) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a general health information assistant for educational purposes only. ")
                .append("Do NOT diagnose, triage, or provide medical, mental health, or treatment advice. ")
                .append("Do NOT estimate urgency or tell the user what condition they have. ")
                .append("You may provide general, non-personalized information, common symptoms, and general 'when to seek professional care' guidance. ")
                .append("If the user describes an emergency, respond with: 'If you think this is an emergency, call your local emergency number.' ")
                .append("Keep responses brief and structured.\n\n");


        sb.append("Conversation (most recent last):\n");

        List<ChatMessage> ctx = getLastTurns(conversation, MAX_CONTEXT_TURNS);
        for (ChatMessage m : ctx) {
            if (m.type == ChatMessage.TYPE_USER) {
                sb.append("User: ").append(safe(m.text)).append("\n");
            } else if (m.type == ChatMessage.TYPE_BOT) {
                sb.append("Assistant: ").append(safe(m.text)).append("\n");
            } else if (m.type == ChatMessage.TYPE_SYSTEM) {
                sb.append("System: ").append(safe(m.text)).append("\n");
            }
        }

        sb.append("\nUser (new message): ").append(safe(newUserText)).append("\n");
        sb.append("Assistant (reply):");

        return sb.toString();
    }

    private List<ChatMessage> getLastTurns(List<ChatMessage> all, int max) {
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<ChatMessage> filtered = new ArrayList<>();
        for (ChatMessage m : all) {
            if (m.type == ChatMessage.TYPE_USER || m.type == ChatMessage.TYPE_BOT || m.type == ChatMessage.TYPE_SYSTEM) {
                filtered.add(m);
            }
        }
        int start = Math.max(0, filtered.size() - max);
        return filtered.subList(start, filtered.size());
    }

    private String postProcessBotReply(String candidate, List<ChatMessage> snapshot) {
        if (TextUtils.isEmpty(candidate)) {
            return "I didn’t catch that. Tell me your main symptom and how long it’s been happening.";
        }

    
        String norm = normalizeForRepeat(candidate);
        if (!TextUtils.isEmpty(lastBotNormalized)) {
            int sim = similarityPercent(lastBotNormalized, norm);
            if (sim >= MIN_REPEAT_SIMILARITY_PERCENT) {
                return buildNonRepeatingFallbackPrompt(snapshot);
            }
        }

        if (conversationContainsNoRedFlags(snapshot) && containsRedFlagQuestion(candidate)) {
            return "Thanks—understood that you’re not noticing urgent red flags. "
                    + "Now tell me: how long has this been going on, how severe is it (0–10), and any key associated symptoms (fever, vomiting, weakness, shortness of breath)?";
        }

        return candidate;
    }

    private boolean conversationContainsNoRedFlags(List<ChatMessage> snapshot) {
        if (snapshot == null) return false;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            ChatMessage m = snapshot.get(i);
            if (m.type != ChatMessage.TYPE_USER || m.text == null) continue;
            String t = m.text.toLowerCase(Locale.US).trim();
            if (t.equals("no red flags") || t.contains("no red flag") || t.contains("none of those") || t.equals("no")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRedFlagQuestion(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.US);
        return t.contains("red flag") || t.contains("any emergency") || t.contains("call your local")
                || t.contains("urgent warning") || t.contains("seek urgent") || t.contains("go to the er");
    }

    private String buildNonRepeatingFallbackPrompt(List<ChatMessage> snapshot) {
        boolean haveDuration = AiDoctorEngine.hasDuration(snapshot);
        boolean haveSeverity = AiDoctorEngine.hasSeverity(snapshot);

        if (!haveDuration && !haveSeverity) {
            return "To help safely: what’s the main symptom, how long it’s been happening, and how severe it is (0–10 or mild/moderate/severe)?";
        }
        if (!haveDuration) {
            return "Thanks. How long has this been happening (hours/days/weeks), and is it getting better, worse, or staying the same?";
        }
        if (!haveSeverity) {
            return "Thanks. How severe is it right now (0–10 or mild/moderate/severe)? Any fever, weakness, or breathing trouble?";
        }
        return "Thanks. Any key associated symptoms (fever, vomiting, weakness, shortness of breath), and do you have any major medical conditions or new medications?";
    }

    private String normalizeForRepeat(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.US)
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
        if (t.length() > 260) t = t.substring(0, 260);
        return t;
    }

    private int similarityPercent(String a, String b) {
        if (TextUtils.isEmpty(a) || TextUtils.isEmpty(b)) return 0;
        Set<String> wa = new HashSet<>(Arrays.asList(a.split(" ")));
        Set<String> wb = new HashSet<>(Arrays.asList(b.split(" ")));
        wa.remove("");
        wb.remove("");
        if (wa.isEmpty() || wb.isEmpty()) return 0;

        int inter = 0;
        for (String w : wa) if (wb.contains(w)) inter++;

        int union = wa.size() + wb.size() - inter;
        if (union <= 0) return 0;

        return Math.round((inter * 100f) / union);
    }

    private String normalizeUserMemoryKey(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.US).trim().replaceAll("\\s+", " ");
    }

    private String safe(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() > 700) t = t.substring(0, 700) + "…";
        return t;
    }

    private void addUserMessage(String text) {
        messages.add(ChatMessage.user(text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private int addBotMessageReturnIndex(String text) {
        messages.add(ChatMessage.bot(text));
        int idx = messages.size() - 1;
        adapter.notifyItemInserted(idx);
        scrollToBottom();
        return idx;
    }

    private void addBotMessage(String text) {
        messages.add(ChatMessage.bot(text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void replaceBotMessage(int index, String newText) {
        if (index < 0 || index >= messages.size()) {
            addBotMessage(newText);
            return;
        }
        boolean expanded = messages.get(index).expanded;
        messages.set(index, ChatMessage.bot(newText));
        messages.get(index).expanded = expanded;
        adapter.notifyItemChanged(index);
        scrollToBottom();
    }

    private void addBotQuickReplies(List<String> replies) {
        messages.add(ChatMessage.quickReplies(Collections.singletonList(replies)));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void replaceWithSystemMessage(int index, String text, boolean showRetry) {
        if (index < 0 || index >= messages.size()) {
            messages.add(ChatMessage.system(text, showRetry));
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return;
        }
        messages.set(index, ChatMessage.system(text, showRetry));
        adapter.notifyItemChanged(index);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (recycler == null) return;
        recycler.post(() -> {
            int last = Math.max(messages.size() - 1, 0);
            recycler.smoothScrollToPosition(last);
        });
    }

    private String findLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m.type == ChatMessage.TYPE_USER && m.text != null) return m.text;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aiExec.shutdownNow();
    }


    static final class ChatMessage {
        static final int TYPE_USER = 0;
        static final int TYPE_BOT = 1;
        static final int TYPE_QUICK_REPLIES = 2;
        static final int TYPE_SYSTEM = 3;

        final int type;
        final String text;

        final List<List<String>> quickReplyGroups;
        final boolean showRetry;

        boolean expanded = false; 

        private ChatMessage(int type, String text, List<List<String>> quickReplyGroups, boolean showRetry) {
            this.type = type;
            this.text = text;
            this.quickReplyGroups = quickReplyGroups;
            this.showRetry = showRetry;
        }

        static ChatMessage user(String text) { return new ChatMessage(TYPE_USER, text, null, false); }
        static ChatMessage bot(String text) { return new ChatMessage(TYPE_BOT, text, null, false); }
        static ChatMessage quickReplies(List<List<String>> groups) { return new ChatMessage(TYPE_QUICK_REPLIES, null, groups, false); }
        static ChatMessage system(String text, boolean retry) { return new ChatMessage(TYPE_SYSTEM, text, null, retry); }
    }


    static final class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        interface Callbacks {
            void onQuickReply(String text);
            void onRetryLast();
            void onToggleBotExpanded(int position);
        }

        private final List<ChatMessage> data;
        private final Callbacks callbacks;

        ChatAdapter(List<ChatMessage> data, Callbacks callbacks) {
            this.data = data;
            this.callbacks = callbacks;
        }

        @Override public int getItemViewType(int position) { return data.get(position).type; }
        @Override public int getItemCount() { return data.size(); }

        @NonNull
        @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == ChatMessage.TYPE_USER) return new UserVH(buildUserRow(parent));
            if (viewType == ChatMessage.TYPE_QUICK_REPLIES) return new QuickRepliesVH(buildQuickRepliesRow(parent));
            if (viewType == ChatMessage.TYPE_SYSTEM) return new SystemVH(buildSystemRow(parent));
            return new BotVH(buildBotRow(parent));
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage m = data.get(position);
            if (holder instanceof UserVH) ((UserVH) holder).bind(m);
            else if (holder instanceof BotVH) ((BotVH) holder).bind(m, position, callbacks);
            else if (holder instanceof QuickRepliesVH) ((QuickRepliesVH) holder).bind(m, callbacks);
            else if (holder instanceof SystemVH) ((SystemVH) holder).bind(m, callbacks);
        }

        private View buildUserRow(ViewGroup parent) {
            FrameLayout root = rowRoot(parent, 4, 10);

            LinearLayout bubble = new LinearLayout(parent.getContext());
            bubble.setOrientation(LinearLayout.VERTICAL);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.END;
            lp.leftMargin = dp(parent, 56);
            bubble.setLayoutParams(lp);

            bubble.setPadding(dp(parent, 14), dp(parent, 12), dp(parent, 14), dp(parent, 12));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF0EA5A4);
            bg.setCornerRadii(new float[]{
                    dp(parent, 18), dp(parent, 18),
                    dp(parent, 6), dp(parent, 6),
                    dp(parent, 18), dp(parent, 18),
                    dp(parent, 18), dp(parent, 18)
            });
            bubble.setBackground(bg);

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setLineSpacing(0f, 1.28f);
            tv.setTextIsSelectable(true);

            bubble.addView(tv);
            root.addView(bubble);
            root.setTag(tv);
            return root;
        }

        private View buildBotRow(ViewGroup parent) {
            FrameLayout root = rowRoot(parent, 4, 10);

            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);

            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            clp.gravity = Gravity.START;
            container.setLayoutParams(clp);

            LinearLayout bubble = new LinearLayout(parent.getContext());
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(dp(parent, 14), dp(parent, 12), dp(parent, 14), dp(parent, 12));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFFFFFFF);
            bg.setStroke(dp(parent, 1), 0x14000000);
            bg.setCornerRadii(new float[]{
                    dp(parent, 10), dp(parent, 10),
                    dp(parent, 18), dp(parent, 18),
                    dp(parent, 18), dp(parent, 18),
                    dp(parent, 18), dp(parent, 18)
            });
            bubble.setBackground(bg);

            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            blp.rightMargin = dp(parent, 24);
            bubble.setLayoutParams(blp);

            LinearLayout bubbleContent = new LinearLayout(parent.getContext());
            bubbleContent.setOrientation(LinearLayout.VERTICAL);
            bubble.addView(bubbleContent);

            TextView toggle = new TextView(parent.getContext());
            toggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            toggle.setTypeface(Typeface.DEFAULT_BOLD);
            toggle.setTextColor(LINK_TEAL);
            toggle.setPadding(0, dp(parent, 10), 0, 0);
            bubble.addView(toggle);

            TextView trust = new TextView(parent.getContext());
            trust.setText("Information only — not medical advice, diagnosis, or treatment. For concerns, consult a qualified clinician.");
            trust.setTextColor(0xFF6B7280);
            trust.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            trust.setLineSpacing(0f, 1.2f);

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tlp.topMargin = dp(parent, 6);
            trust.setLayoutParams(tlp);

            container.addView(bubble);
            container.addView(trust);

            root.addView(container);
            root.setTag(new Object[]{bubble, bubbleContent, toggle, trust});
            return root;
        }

        private View buildSystemRow(ViewGroup parent) {
            FrameLayout root = rowRoot(parent, 6, 12);

            LinearLayout pill = new LinearLayout(parent.getContext());
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            pill.setPadding(dp(parent, 12), dp(parent, 10), dp(parent, 12), dp(parent, 10));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            pill.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF3F4F6);
            bg.setCornerRadius(dp(parent, 16));
            bg.setStroke(dp(parent, 1), 0x14000000);
            pill.setBackground(bg);

            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(0xFF374151);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setLineSpacing(0f, 1.2f);

            TextView retry = new TextView(parent.getContext());
            retry.setText("  Retry");
            retry.setTextColor(0xFF0EA5A4);
            retry.setTypeface(Typeface.DEFAULT_BOLD);
            retry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            retry.setVisibility(View.GONE);

            pill.addView(tv);
            pill.addView(retry);

            root.addView(pill);
            root.setTag(new Object[]{tv, retry});
            return root;
        }

        private View buildQuickRepliesRow(ViewGroup parent) {
            FrameLayout root = rowRoot(parent, 2, 12);

            android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(parent.getContext());
            hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            hsv.setHorizontalScrollBarEnabled(false);

            ChipGroup cg = new ChipGroup(parent.getContext());
            cg.setSingleLine(true);
            cg.setChipSpacingHorizontal(dp(parent, 8));
            cg.setPadding(dp(parent, 2), 0, dp(parent, 2), 0);

            hsv.addView(cg, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            root.addView(hsv);
            root.setTag(cg);
            return root;
        }

        private FrameLayout rowRoot(ViewGroup parent, int topDp, int bottomDp) {
            FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            root.setPadding(0, dp(parent, topDp), 0, dp(parent, bottomDp));
            return root;
        }

        static final class UserVH extends RecyclerView.ViewHolder {
            private final TextView tv;
            UserVH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView.getTag();
            }
            void bind(ChatMessage m) { tv.setText(m.text == null ? "" : m.text); }
        }

        static final class BotVH extends RecyclerView.ViewHolder {
            private final LinearLayout bubble;
            private final LinearLayout bubbleContent;
            private final TextView toggle;
            private final TextView trust;

            BotVH(@NonNull View itemView) {
                super(itemView);
                Object[] tags = (Object[]) itemView.getTag();
                bubble = (LinearLayout) tags[0];
                bubbleContent = (LinearLayout) tags[1];
                toggle = (TextView) tags[2];
                trust = (TextView) tags[3];
            }

            void bind(ChatMessage m, int position, Callbacks callbacks) {
                String raw = m.text == null ? "" : m.text.trim();
                bubbleContent.removeAllViews();

                FormattedBot fb = BotFormatter.format(raw);
                boolean hasRedFlags = fb.redFlags != null && !fb.redFlags.isEmpty();

                applyBotBubbleSemanticStyle(bubble, hasRedFlags);

                boolean showToggle = shouldShowToggle(raw, fb, hasRedFlags);
                toggle.setVisibility(showToggle ? View.VISIBLE : View.GONE);

                if (!showToggle) {
                    renderExpandedContent(raw, fb, hasRedFlags);
                    toggle.setOnClickListener(null);
                    trust.setVisibility(shouldShowTrustNote(raw, hasRedFlags) ? View.VISIBLE : View.GONE);
                    return;
                }

                if (!m.expanded) {
                    String summary = buildSummary(fb, hasRedFlags);
                    bubbleContent.addView(buildBotText(bubbleContent, summary));
                    toggle.setText("More details");
                } else {
                    renderExpandedContent(raw, fb, hasRedFlags);
                    toggle.setText("Show less");
                }

                toggle.setOnClickListener(v -> {
                    if (callbacks != null) callbacks.onToggleBotExpanded(position);
                });

                trust.setVisibility(shouldShowTrustNote(raw, hasRedFlags) ? View.VISIBLE : View.GONE);
            }

            private boolean shouldShowToggle(String raw, FormattedBot fb, boolean hasRedFlags) {
                if (hasRedFlags) return true;
                if (raw == null) return false;
                if (raw.length() > 240) return true;
                return fb != null && !TextUtils.isEmpty(fb.closing);
            }

            private String buildSummary(FormattedBot fb, boolean hasRedFlags) {
                String intro = (fb == null || fb.intro == null) ? "" : fb.intro.trim();
                if (intro.length() > 180) intro = intro.substring(0, 180) + "…";

                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(intro)) sb.append(intro);

                if (hasRedFlags) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append("Seek urgent care if:\n");
                    int count = Math.min(2, fb.redFlags.size());
                    for (int i = 0; i < count; i++) {
                        sb.append("• ").append(fb.redFlags.get(i)).append("\n");
                    }
                    return sb.toString().trim();
                }

                if (sb.length() == 0) sb.append("Tell me the duration and severity.");
                return sb.toString().trim();
            }

            private void renderExpandedContent(String raw, FormattedBot fb, boolean hasRedFlags) {
                if (!TextUtils.isEmpty(fb.intro)) bubbleContent.addView(buildBotText(bubbleContent, fb.intro));

                if (hasRedFlags) {
                    bubbleContent.addView(spacer(bubbleContent, 8));
                    bubbleContent.addView(buildRedFlagsHeader(bubbleContent));
                    bubbleContent.addView(spacer(bubbleContent, 6));
                    for (String rf : fb.redFlags) bubbleContent.addView(buildBullet(bubbleContent, rf));
                }

                if (!TextUtils.isEmpty(fb.closing)) {
                    bubbleContent.addView(spacer(bubbleContent, 8));
                    bubbleContent.addView(buildBotText(bubbleContent, fb.closing));
                }

                if (bubbleContent.getChildCount() == 0) {
                    bubbleContent.addView(buildBotText(bubbleContent, raw));
                }
            }

            private boolean shouldShowTrustNote(String raw, boolean hasRedFlags) {
                if (hasRedFlags) return true;
                if (raw == null) return false;
                String t = raw.toLowerCase(Locale.US);
                if (t.contains("emergency") || t.contains("call your local") || t.contains("urgent")) return true;
                return raw.length() > 240;
            }

            private void applyBotBubbleSemanticStyle(LinearLayout bubble, boolean warn) {
                if (!(bubble.getBackground() instanceof GradientDrawable)) return;
                GradientDrawable bg = (GradientDrawable) bubble.getBackground();

                if (warn) {
                    bg.setColor(0xFFFFFBEB);
                    bg.setStroke(dp(bubble, 1), 0x33F59E0B);
                } else {
                    bg.setColor(0xFFFFFFFF);
                    bg.setStroke(dp(bubble, 1), 0x14000000);
                }
            }

            private View buildBotText(ViewGroup parent, String text) {
                TextView tv = new TextView(parent.getContext());
                tv.setText(text);
                tv.setTextColor(0xFF111827);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv.setLineSpacing(0f, 1.30f);
                tv.setTextIsSelectable(true);

                tv.setAutoLinkMask(Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);
                tv.setLinksClickable(true);
                tv.setMovementMethod(LinkMovementMethod.getInstance());

                return tv;
            }

            private View buildRedFlagsHeader(ViewGroup parent) {
                LinearLayout row = new LinearLayout(parent.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView icon = new TextView(parent.getContext());
                icon.setText("⚠");
                icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

                TextView label = new TextView(parent.getContext());
                label.setText("Red flags");
                label.setTextColor(0xFFF59E0B);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                label.setTypeface(Typeface.DEFAULT_BOLD);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.leftMargin = dp(parent, 6);
                label.setLayoutParams(lp);

                row.addView(icon);
                row.addView(label);

                LinearLayout wrap = new LinearLayout(parent.getContext());
                wrap.setOrientation(LinearLayout.VERTICAL);
                wrap.addView(row);

                View divider = new View(parent.getContext());
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(parent, 1)
                );
                dlp.topMargin = dp(parent, 6);
                divider.setLayoutParams(dlp);
                divider.setBackgroundColor(0x33F59E0B);
                wrap.addView(divider);

                return wrap;
            }

            private View buildBullet(ViewGroup parent, String text) {
                TextView tv = new TextView(parent.getContext());
                tv.setText("• " + text);
                tv.setTextColor(0xFF111827);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv.setLineSpacing(0f, 1.25f);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.topMargin = dp(parent, 4);
                tv.setLayoutParams(lp);
                return tv;
            }

            private View spacer(ViewGroup parent, int dp) {
                View v = new View(parent.getContext());
                v.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(parent, dp)
                ));
                return v;
            }
        }

        static final class QuickRepliesVH extends RecyclerView.ViewHolder {
            private final ChipGroup cg;
            QuickRepliesVH(@NonNull View itemView) {
                super(itemView);
                cg = (ChipGroup) itemView.getTag();
            }

            void bind(ChatMessage m, Callbacks callbacks) {
                cg.removeAllViews();
                if (m.quickReplyGroups == null) return;

                List<String> all = new ArrayList<>();
                for (List<String> g : m.quickReplyGroups) all.addAll(g);

                for (String text : all) {
                    Chip chip = buildQuickReplyChip(cg.getContext(), text);
                    chip.setOnClickListener(v -> {
                        if (callbacks != null) callbacks.onQuickReply(text);
                    });
                    cg.addView(chip);
                }
            }

            private static Chip buildQuickReplyChip(Context ctx, String text) {
                Chip c = new Chip(ctx);
                c.setText(text);
                c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                c.setEnsureMinTouchTargetSize(true);

                c.setClickable(true);
                c.setFocusable(true);
                c.setCheckable(false);
                c.setCheckedIconVisible(false);
                c.setCloseIconVisible(false);
                c.setChipIconVisible(false);

                c.setChipCornerRadius(dp(ctx, 999));
                c.setRippleColor(ColorStateList.valueOf(CHIP_RIPPLE));

                c.setChipBackgroundColor(ColorStateList.valueOf(CHIP_BG_TEAL_ALT));
                c.setChipStrokeColor(ColorStateList.valueOf(CHIP_STROKE));
                c.setChipStrokeWidth(dp(ctx, 1));
                c.setTextColor(CHIP_TEXT_WHITE);

                c.setMinHeight(dp(ctx, 36));
                c.setPadding(dp(ctx, 10), dp(ctx, 2), dp(ctx, 10), dp(ctx, 2));
                return c;
            }

            private static int dp(Context ctx, int dp) {
                float d = ctx.getResources().getDisplayMetrics().density;
                return Math.round(dp * d);
            }
        }

        static final class SystemVH extends RecyclerView.ViewHolder {
            private final TextView tv;
            private final TextView retry;

            SystemVH(@NonNull View itemView) {
                super(itemView);
                Object[] tag = (Object[]) itemView.getTag();
                tv = (TextView) tag[0];
                retry = (TextView) tag[1];
            }

            void bind(ChatMessage m, Callbacks callbacks) {
                tv.setText(m.text == null ? "" : m.text);

                if (m.showRetry) {
                    retry.setVisibility(View.VISIBLE);
                    retry.setOnClickListener(v -> {
                        if (callbacks != null) callbacks.onRetryLast();
                    });
                } else {
                    retry.setVisibility(View.GONE);
                    retry.setOnClickListener(null);
                }
            }
        }

        private static int dp(View v, int dp) {
            float d = v.getResources().getDisplayMetrics().density;
            return Math.round(dp * d);
        }
    }


    static final class FormattedBot {
        final String intro;
        final List<String> redFlags;
        final String closing;
        FormattedBot(String intro, List<String> redFlags, String closing) {
            this.intro = intro;
            this.redFlags = redFlags;
            this.closing = closing;
        }
    }

    static final class BotFormatter {
        static FormattedBot format(String raw) {
            if (raw == null) raw = "";
            String text = raw.trim();
            if (text.isEmpty()) return new FormattedBot("", Collections.emptyList(), "");

            String[] lines = text.split("\\r?\\n");
            List<String> cleaned = new ArrayList<>();
            for (String l : lines) {
                String s = l.trim();
                if (!s.isEmpty()) cleaned.add(s);
            }

            int rfIdx = -1;
            for (int i = 0; i < cleaned.size(); i++) {
                String s = cleaned.get(i).toLowerCase(Locale.US);
                if (s.contains("red flag")) { rfIdx = i; break; }
                if (s.startsWith("⚠") && s.contains("flag")) { rfIdx = i; break; }
            }

            if (rfIdx >= 0) {
                String intro = join(cleaned.subList(0, rfIdx)).trim();

                List<String> redFlags = new ArrayList<>();
                int i = rfIdx + 1;
                for (; i < cleaned.size(); i++) {
                    String s = cleaned.get(i);
                    if (looksLikeClosingStart(s)) break;
                    String bullet = s.replaceFirst("^[-•\\u2022]+\\s*", "").trim();
                    if (!bullet.isEmpty()) redFlags.add(bullet);
                }

                String closing = join(cleaned.subList(i, cleaned.size())).trim();

                if (intro.isEmpty()) intro = firstSentence(text);
                if (closing.isEmpty()) {
                    closing = "If symptoms worsen or you feel unsafe, seek medical care. Tell me the duration and severity.";
                }

                return new FormattedBot(intro, redFlags, closing);
            }

            if (cleaned.size() <= 2) return new FormattedBot(text, Collections.emptyList(), "");

            String intro = cleaned.get(0);
            String closing = join(cleaned.subList(1, cleaned.size())).trim();

            List<String> inferred = inferRedFlags(text);
            if (!inferred.isEmpty()) return new FormattedBot(intro, inferred, closing);

            return new FormattedBot(intro, Collections.emptyList(), closing);
        }

        private static boolean looksLikeClosingStart(String s) {
            String t = s.toLowerCase(Locale.US);
            return t.startsWith("if ")
                    || t.contains("seek medical care")
                    || t.contains("call your local")
                    || t.contains("go to the er")
                    || t.contains("tell me")
                    || t.contains("next");
        }

        private static String join(List<String> lines) {
            if (lines == null || lines.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append("\n\n");
                sb.append(lines.get(i));
            }
            return sb.toString();
        }

        private static String firstSentence(String text) {
            String t = text.trim();
            int dot = t.indexOf('.');
            if (dot > 0 && dot < 220) return t.substring(0, dot + 1);
            return t;
        }

        private static List<String> inferRedFlags(String text) {
            String t = (text == null ? "" : text.toLowerCase(Locale.US));
            List<String> rf = new ArrayList<>();
            if (t.contains("chest pain")) rf.add("Chest pain (pressure/tightness, sweating, nausea)");
            if (t.contains("shortness of breath") || t.contains("difficulty breathing")) rf.add("Shortness of breath at rest");
            if (t.contains("faint") || t.contains("passed out")) rf.add("Fainting or near-fainting");
            if (t.contains("worst headache") || t.contains("sudden severe headache")) rf.add("Sudden severe headache");
            if (t.contains("confusion") || t.contains("trouble speaking") || t.contains("face droop"))
                rf.add("Confusion, trouble speaking, or one-sided weakness");
            return rf;
        }
    }


    static final class QuickReplies {

        static List<String> suggest(String aiText, List<ChatMessage> conversation, Set<String> userReplyMemory) {
            String t = aiText == null ? "" : aiText.toLowerCase(Locale.US);

            boolean askedAboutRedFlags = t.contains("red flag")
                    || t.contains("urgent")
                    || t.contains("emergency")
                    || t.contains("call your local")
                    || t.contains("go to the er");

            boolean alreadySaidNoRedFlags = hasUserSaid(conversation, "no red flags")
                    || (userReplyMemory != null && userReplyMemory.contains("no red flags"));

            List<String> out = new ArrayList<>();

            if (askedAboutRedFlags && !alreadySaidNoRedFlags) {
                out.add("No red flags");
            }

            out.add("General information");
            out.add("Self-care basics (general)");
            out.add("When to seek professional care (general)");
            out.add("Questions to ask a clinician");


            if (t.contains("sleep")) out.add("Help me build a sleep routine");
            if (t.contains("stress")) out.add("Give me stress reduction options");
            if (t.contains("nutrition")) out.add("Explain healthy meal structure");


            return dedupeLimit(out, 4);
        }

        private static boolean hasUserSaid(List<ChatMessage> conversation, String exactLower) {
            if (conversation == null) return false;
            for (int i = conversation.size() - 1; i >= 0; i--) {
                ChatMessage m = conversation.get(i);
                if (m.type != ChatMessage.TYPE_USER || m.text == null) continue;
                String u = m.text.toLowerCase(Locale.US).trim();
                if (u.equals(exactLower)) return true;
            }
            return false;
        }

        private static List<String> dedupeLimit(List<String> in, int limit) {
            List<String> out = new ArrayList<>();
            for (String s : in) {
                if (s == null) continue;
                if (!out.contains(s)) out.add(s);
                if (out.size() >= limit) break;
            }
            return out;
        }
    }


    public static class AiDoctorEngine {

        public static String respond(String userInput, List<ChatMessage> conversation) {
            String raw = userInput == null ? "" : userInput.trim();
            String t = normalize(raw);

            if (containsAny(t, "chest pain", "can't breathe", "cant breathe", "unconscious", "stroke", "severe bleeding")) {
                return "I can’t assess emergencies. If you believe this may be urgent, contact local emergency services or a qualified clinician now.";
            }

            if (containsAny(t, "suicide", "kill myself", "self harm", "self-harm")) {
                return "I’m sorry you’re going through this. I can’t help with self-harm content. "
                        + "If you feel at risk, contact local emergency services now or reach out to a trusted person. "
                        + "If you share your country, I can provide crisis hotline information.";
            }

            if (containsAny(t, "sleep", "insomnia")) {
                return "General sleep guidance:\n"
                        + "• Keep a consistent sleep/wake time\n"
                        + "• Reduce caffeine late in the day\n"
                        + "• Keep the room dark and cool\n"
                        + "• Limit screens 60 minutes before bed\n\n"
                        + "If sleep problems persist or affect daily function, consider speaking with a clinician.";
            }

            if (containsAny(t, "stress", "anxiety", "panic")) {
                return "General stress management options:\n"
                        + "• Slow breathing (e.g., 4–6 breaths/min for a few minutes)\n"
                        + "• Short walk or light activity\n"
                        + "• Reduce stimulants (caffeine/nicotine)\n"
                        + "• Journaling or guided relaxation\n\n"
                        + "If you’re struggling to cope or symptoms are severe, consider speaking with a qualified professional.";
            }

            if (containsAny(t, "diet", "nutrition", "weight", "bmi")) {
                return "General nutrition guidance:\n"
                        + "• Prioritize minimally processed foods\n"
                        + "• Include protein + fiber at most meals\n"
                        + "• Aim for regular meals and hydration\n\n"
                        + "For individualized goals or medical conditions, consult a registered dietitian or clinician.";
            }

            if (containsAny(t, "exercise", "fitness", "activity")) {
                return "General activity guidance:\n"
                        + "• Start with achievable goals (e.g., 10–20 minutes/day)\n"
                        + "• Combine cardio + strength across the week\n"
                        + "• Increase gradually to avoid injury\n\n"
                        + "If you have health concerns, ask a clinician before major changes.";
            }

            return "I can share general health and wellness information. "
                    + "Tell me the topic you want to learn about (sleep, nutrition, activity, stress, or understanding common symptoms in general terms).";
        }

        private static String normalize(String s) {
            return s == null ? "" : s.trim().toLowerCase(Locale.US);
        }

        private static boolean containsAny(String t, String... needles) {
            if (t == null) return false;
            for (String n : needles) if (t.contains(n)) return true;
            return false;
        }
        static boolean hasDuration(List<ChatMessage> conversation) {
            if (conversation == null) return false;
            for (int i = conversation.size() - 1; i >= 0; i--) {
                ChatMessage m = conversation.get(i);
                if (m.type != ChatMessage.TYPE_USER || m.text == null) continue;
                if (containsDurationRaw(m.text)) return true;
            }
            return false;
        }

        static boolean hasSeverity(List<ChatMessage> conversation) {
            if (conversation == null) return false;
            for (int i = conversation.size() - 1; i >= 0; i--) {
                ChatMessage m = conversation.get(i);
                if (m.type != ChatMessage.TYPE_USER || m.text == null) continue;
                if (containsSeverityRaw(m.text)) return true;
            }
            return false;
        }

        private static boolean containsDurationRaw(String text) {
            String t = normalize(text);
            return t.matches(".*\\b\\d+\\s*(min|mins|minute|minutes|hr|hrs|hour|hours|day|days|week|weeks|month|months)\\b.*")
                    || t.contains("today")
                    || t.contains("yesterday")
                    || t.contains("since");
        }

        private static boolean containsSeverityRaw(String text) {
            String t = normalize(text);
            if (t.contains("mild") || t.contains("moderate") || t.contains("severe")) return true;
            return t.matches(".*\\b\\d{1,2}\\s*/\\s*10\\b.*") || t.matches(".*\\b\\d{1,2}\\s*of\\s*10\\b.*");
        }

    }


    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}
