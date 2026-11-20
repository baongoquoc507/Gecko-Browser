package com.example.geckobrowser;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;
import java.util.List;

public class BrowserPromptDelegate implements GeckoSession.PromptDelegate {

    public interface FilePickerHost {
        void onRequestFilePicker(
                @NonNull FilePrompt prompt,
                @NonNull GeckoResult<PromptResponse> result
        );
    }

    private final Activity activity;
    @Nullable
    private final FilePickerHost filePickerHost;

    public BrowserPromptDelegate(Activity activity, @Nullable FilePickerHost filePickerHost) {
        this.activity = activity;
        this.filePickerHost = filePickerHost;
    }

    private boolean isActivityUnavailable() {
        if (activity.isFinishing()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onAlertPrompt(@NonNull GeckoSession session, @NonNull final AlertPrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable()) {
                    result.complete(prompt.dismiss());
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .show();
            }
        });
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onButtonPrompt(@NonNull GeckoSession session, @NonNull final ButtonPrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable()) {
                    result.complete(prompt.dismiss());
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE));
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .show();
            }
        });
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onTextPrompt(@NonNull GeckoSession session, @NonNull final TextPrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable()) {
                    result.complete(prompt.dismiss());
                    return;
                }
                final EditText input = new EditText(activity);
                if (prompt.defaultValue != null) {
                    input.setText(prompt.defaultValue);
                }
                int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
                LinearLayout container = new LinearLayout(activity);
                container.setPadding(pad, pad / 2, pad, 0);
                container.addView(input, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                new AlertDialog.Builder(activity)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setView(container)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.confirm(input.getText().toString()));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .show();
            }
        });
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onChoicePrompt(@NonNull GeckoSession session, @NonNull final ChoicePrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable() || prompt.choices == null || prompt.choices.length == 0) {
                    result.complete(prompt.dismiss());
                    return;
                }
                final ChoicePrompt.Choice[] choices = prompt.choices;
                final String[] labels = new String[choices.length];
                final boolean[] checked = new boolean[choices.length];
                int selectedIdx = -1;
                for (int i = 0; i < choices.length; i++) {
                    labels[i] = choices[i].label != null ? choices[i].label : "";
                    checked[i] = choices[i].selected;
                    if (choices[i].selected && selectedIdx == -1) {
                        selectedIdx = i;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                if (prompt.title != null && !prompt.title.isEmpty()) {
                    builder.setTitle(prompt.title);
                }

                if (prompt.type == ChoicePrompt.Type.MULTIPLE) {
                    builder.setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            checked[which] = isChecked;
                        }
                    });
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            List<String> selectedIds = new ArrayList<>();
                            for (int i = 0; i < choices.length; i++) {
                                if (checked[i] && choices[i].id != null) {
                                    selectedIds.add(choices[i].id);
                                }
                            }
                            result.complete(prompt.confirm(selectedIds.toArray(new String[0])));
                        }
                    });
                } else {
                    builder.setSingleChoiceItems(labels, selectedIdx, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            result.complete(prompt.confirm(choices[which]));
                        }
                    });
                }

                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.complete(prompt.dismiss());
                    }
                });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        result.complete(prompt.dismiss());
                    }
                });
                builder.show();
            }
        });
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onAuthPrompt(@NonNull GeckoSession session, @NonNull final AuthPrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable()) {
                    result.complete(prompt.dismiss());
                    return;
                }
                int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(pad, pad / 2, pad, 0);

                final boolean onlyPassword = (prompt.authOptions.flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) != 0;
                final EditText userInput = new EditText(activity);
                userInput.setHint(R.string.auth_username_hint);
                if (prompt.authOptions.username != null) {
                    userInput.setText(prompt.authOptions.username);
                }
                if (!onlyPassword) {
                    layout.addView(userInput);
                }

                final EditText passInput = new EditText(activity);
                passInput.setHint(R.string.auth_password_hint);
                passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                if (prompt.authOptions.password != null) {
                    passInput.setText(prompt.authOptions.password);
                }
                layout.addView(passInput);

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.auth_title)
                        .setMessage(prompt.message)
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String pass = passInput.getText().toString();
                                if (onlyPassword) {
                                    result.complete(prompt.confirm(pass));
                                } else {
                                    result.complete(prompt.confirm(userInput.getText().toString(), pass));
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                result.complete(prompt.dismiss());
                            }
                        })
                        .show();
            }
        });
        return result;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onFilePrompt(@NonNull GeckoSession session, @NonNull final FilePrompt prompt) {
        final GeckoResult<PromptResponse> result = new GeckoResult<>();
        if (filePickerHost == null || isActivityUnavailable()) {
            result.complete(prompt.dismiss());
            return result;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActivityUnavailable()) {
                    result.complete(prompt.dismiss());
                } else {
                    filePickerHost.onRequestFilePicker(prompt, result);
                }
            }
        });
        return result;
    }
}
