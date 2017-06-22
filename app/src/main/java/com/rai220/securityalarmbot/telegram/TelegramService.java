package com.rai220.securityalarmbot.telegram;

import android.app.AlertDialog;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.base.Strings;
import com.pengrad.telegrambot.GetUpdatesListener;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendLocation;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVoice;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.CommandHelper;
import com.rai220.securityalarmbot.exeptions.NoCommandException;
import com.rai220.securityalarmbot.model.IncomingMessage;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.Constants;
import com.rai220.securityalarmbot.utils.Converters;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramService implements ISenderService {

    private final ExecutorService es = Executors.newCachedThreadPool();
    private IStartService startService;
    private BotService botService;
    private TelegramBot bot = null;
    private CommandHelper commandHelper;
    private volatile boolean isRunning = false;

    public TelegramService(IStartService startService) {
        this.startService = startService;
    }

    public void init(final BotService botService) {
        this.botService = botService;
        this.commandHelper = new CommandHelper(botService);
        final Keyboard mainKeyboard = commandHelper.getMainKeyboard();
        bot = TelegramBotAdapter.build(PrefsController.instance.getToken());
        es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    GetMeResponse getMeResponse = bot.execute(new GetMe());
                    isRunning = getMeResponse.isOk();
                    if (isRunning) {
                        L.i("----- BOT IS OK -----");
                        startService.onStartSuccess();

                        commandHelper.init();
                        GetUpdates getUpdates = new GetUpdates().timeout(5);
                        bot.setGetUpdatetsListener(new GetUpdatesListener() {
                            @Override
                            public int process(List<Update> updates) {
                                try {
                                    for (Update update : updates) {
                                        Message message = update.message();
                                        final String textMessage = message.text();
                                        final long chatId = message.chat().id();
                                        final Prefs prefs = PrefsController.instance.getPrefs();
                                        final String commandsStr = Converters.COMMANDS_TO_STRING.apply(commandHelper.getRegisteredCommands());
                                        final Prefs.UserPrefs userPrefs = prefs.getUser(message.from());
                                        if (userPrefs == null) {
                                            if (FabricUtils.isPassCorrect(textMessage, prefs.password)) {
                                                Prefs.UserPrefs user = prefs.addUser(message.from(), chatId);
                                                PrefsController.instance.setPrefs(prefs);
                                                sendMessage(chatId, botService.getString(R.string.access_granted));
                                                sendMessage(chatId, botService.getString(R.string.please_select_command) + commandsStr, mainKeyboard);
                                                notifyToOthers(user.id, botService.getString(R.string.user_id_granted_access));
                                            } else {
                                                sendMessage(chatId, botService.getString(R.string.enter_correct_password));
                                                String userName = !Strings.isNullOrEmpty(message.from().username()) ?
                                                        "@" + message.from().username() :
                                                        message.from().firstName() + " " + message.from().lastName() + " (" + message.from().id() + ")";
                                                sendMessageToAll(userName + botService.getString(R.string.tries_get_access));
                                            }
                                        } else {
                                            if (prefs.updateUser(message.from())) {
                                                PrefsController.instance.setPrefs(prefs);
                                            }
                                            try {
                                                commandHelper.executeCommand(message, prefs);
                                            } catch (NoCommandException ex) {
//                                                sendMessage(chatId, botService.getString(R.string.no_implementation_command));
                                                sendMessage(chatId, botService.getString(R.string.please_select_command) + commandsStr, mainKeyboard);
                                            }
                                            Answers.getInstance().logCustom(new CustomEvent("Message").putCustomAttribute("text", "" + textMessage));
                                        }
                                    }
                                    return GetUpdatesListener.CONFIRMED_UPDATES_ALL;
                                } catch (Throwable ex) {
                                    L.e(ex);
                                    return GetUpdatesListener.CONFIRMED_UPDATES_ALL;
                                }
                            }
                        }, getUpdates);

                        sendMessageToAll(botService.getString(R.string.bot_started), mainKeyboard);
                    } else {
                        L.i("----- BOT FAILED -----");
                        startService.onStartFailed();
                    }
                } catch (Throwable ex) {
                    botService.stopSelf();

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog alertDialog = new AlertDialog.Builder(botService)
                                    .setTitle("Error!")
                                    .setMessage(R.string.error_internet_trouble)
                                    .create();

                            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            alertDialog.show();
                        }
                    });
                }
            }
        });
    }

    public void stop() {
        bot.removeGetUpdatesListener();
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public TelegramBot getBot() {
        return bot;
    }

    @Override
    public void sendDocument(final Long id, final byte[] document, final String fileName) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                bot.execute(new SendDocument(id, document).fileName(fileName));
            }
        });
    }

    @Override
    public void sendMessage(final Long id, final String message) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                bot.execute(new SendMessage(id, message));
            }
        });
    }

    @Override
    public void sendMessage(final Long id, final String message, final Keyboard keyboard) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                bot.execute(new SendMessage(id, message).replyMarkup(keyboard));
            }
        });
    }

    @Override
    public void sendMessageToAll(final String message) {
        final Set<Prefs.UserPrefs> subscribers = PrefsController.instance.getPrefs().getEventListeners();
        if (!subscribers.isEmpty()) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    for (final Prefs.UserPrefs user : subscribers) {
                        bot.execute(new SendMessage(user.lastChatId, message));
                    }
                }
            });
        }
    }

    @Override
    public void sendMessageToAll(final String message, final Keyboard keyboard) {
        final Set<Prefs.UserPrefs> subscribers = PrefsController.instance.getPrefs().getEventListeners();
        es.submit(new Runnable() {
            @Override
            public void run() {
                for (final Prefs.UserPrefs user : subscribers) {
                    bot.execute(new SendMessage(user.lastChatId, message).replyMarkup(keyboard));
                }
            }
        });
    }

    @Override
    public void sendMessageToAll(final List<IncomingMessage> incomingMessageList) {
        final Set<Prefs.UserPrefs> subscribers = PrefsController.instance.getPrefs().getEventListeners();
        es.submit(new Runnable() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(botService.getString(com.rai220.securityalarmbot.R.string.incoming_messages));
                for (IncomingMessage message : incomingMessageList) {
                    stringBuilder.append(message.getPhone());
                    if (!Strings.isNullOrEmpty(message.getName())) {
                        stringBuilder.append(" (").append(message.getName()).append(")");
                    }
                    stringBuilder.append(": \"").append(message.getMessage()).append("\"\n");
                }

                for (final Prefs.UserPrefs user : subscribers) {
                    bot.execute(new SendMessage(user.lastChatId, stringBuilder.toString()));
                }
            }
        });
    }

    @Override
    public void notifyToOthers(final int userId, final String message) {
        Prefs prefs = PrefsController.instance.getPrefs();
        final Prefs.UserPrefs userChange = prefs.getUser(userId);
        final Set<Prefs.UserPrefs> subscribers = prefs.getEventListeners();
        es.submit(new Runnable() {
            @Override
            public void run() {
                String userName = "null";
                if (userChange != null) {
                    userName = userChange.isNick ? "@".concat(userChange.userName) : userChange.userName;
                }
                for (final Prefs.UserPrefs user : subscribers) {
                    if (user.id != userId) {
                        bot.execute(new SendMessage(user.lastChatId, userName + " " + message));
                    }
                }
            }
        });
    }

    @Override
    public void sendPhoto(final Long id, final byte[] photo) {
        sendPhoto(id, photo, null);
    }

    @Override
    public void sendPhoto(final Long id, final byte[] photo, final String caption) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                sendOnePhoto(id, photo, caption);
            }
        });
    }

    @Override
    public void sendPhotoToAll(final byte[] photo) {
        sendPhotoToAll(photo, null);
    }

    @Override
    public void sendPhotoToAll(final byte[] photo, final String caption) {
        final Set<Prefs.UserPrefs> subscribers = PrefsController.instance.getPrefs().getEventListeners();
        es.submit(new Runnable() {
            @Override
            public void run() {
                for (final Prefs.UserPrefs user : subscribers) {
                    sendOnePhoto(user.lastChatId, photo, caption);
                }
            }
        });
    }

    @Override
    public void sendLocation(final Long chatId, final Location location) {
        if (location != null) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    float latitude = (float) location.getLatitude();
                    float longitude = (float) location.getLongitude();
                    DateTime date = new DateTime(location.getTime());
                    if (date.isBefore(DateTime.now().minusMinutes(60))) {
                        bot.execute(new SendMessage(chatId, botService.getString(R.string.last_date_location) +
                                date.toString(Constants.DATE_TIME_PATTERN)));
                    }
                    bot.execute(new SendLocation(chatId, latitude, longitude));
                }
            });
        }
    }

    @Override
    public void sendAudio(final Long chatId, final File file) {
        if (file != null) {
            L.i(String.format("Send audio file '%s', (isExists %s)", file.getName(), file.exists()));
            if (file.exists()) {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        DateTime date = new DateTime(file.lastModified());
                        date.withZone(DateTimeZone.UTC);
                        bot.execute(new SendVoice(chatId, file).caption(date.toString(Constants.DATE_TIME_PATTERN)));
                        L.i("Is file deleted: " + file.delete());
                    }
                });
            }
        }
    }

    private void sendOnePhoto(Long id, byte[] photo, String caption) {
        SendPhoto request = new SendPhoto(id, photo);
        if (caption != null) {
            request.caption(caption);
        }
        bot.execute(request);
        L.i("Sent photo successful to " + id);
    }
}
