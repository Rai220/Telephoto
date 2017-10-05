package com.rai220.securityalarmbot.commands;

import com.google.common.collect.ImmutableSet;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.exeptions.NoCommandException;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.telegram.TelegramService;
import com.rai220.securityalarmbot.utils.KeyboardUtils;
import com.rai220.securityalarmbot.utils.L;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */

public class CommandHelper {

    private final BotService botService;
    private final TelegramService telegramService;
    /**
     * Map of registered commands
     */
    private Map<String, ICommand> registeredCommandMap = new HashMap<>();
    /**
     * Map of last user's command
     * key - User
     * value - last command
     */
    private Map<Prefs.UserPrefs, ICommand> lastUserCommand = new HashMap<>();

    private Keyboard mainKeyboard;

    public CommandHelper(BotService botService) {
        this.botService = botService;
        this.telegramService = botService.getTelegramService();
        init();
    }

    public void init() {
        registeredCommandMap.clear();
        lastUserCommand.clear();

        Set<Class<? extends AbstractCommand>> existsCommands = ImmutableSet.of(
                PhotoCommand.class,
                HDPhotoCommand.class,
                CameraModeCommand.class,
                CameraSettingsCommand.class,
                AlarmSettingsCommand.class,
                MinutesCommand.class,
                HistoryCommand.class,
                MotionDetectCommand.class,
                MDSensitivityCommand.class,
                MdModeCommand.class,
                ShakeSensorCommand.class,
                ShakeSensitivityCommand.class,
                GpsCommand.class,
                NotificationCommand.class,
                TtsCommand.class,
                AudioCommand.class,
                TaskerCommand.class,
                LogCommand.class,
                SetMatrixCommand.class,
                // ProCommand.class, // Temporary disable this function
                RestartCommand.class,
                TimeStatsCommand.class,
                TimeStatsClearCommand.class,
                StatusCommand.class,
                HelpCommand.class
        );

        for (Class<? extends AbstractCommand> clazz : existsCommands) {
            ICommand instance = createInstance(clazz);
            if (registeredCommandMap.containsKey(instance.getCommand())) {
                ICommand command = registeredCommandMap.get(instance.getCommand());
                L.e("Duplicate of command: " + instance.getCommand() +
                        ", names: [" + instance.getName() + ", " + command.getName() + "]");
            } else {
                registeredCommandMap.put(instance.getCommand(), instance);
            }
        }
        mainKeyboard = KeyboardUtils.getKeyboard(registeredCommandMap.values());
        L.i("Count of registered commands: " + registeredCommandMap.size());
    }

    public void executeCommand(Message message, Prefs prefs) throws NoCommandException {
        final long chatId = message.chat().id();
        String text = message.text();
        Prefs.UserPrefs user = prefs.getUser(message.from());

        ICommand command = findCommand(text);
        if (command == null) {
            command = lastUserCommand.get(user);
            lastUserCommand.remove(user);
            if (command == null) {
                throw new NoCommandException(text);
            }
        }

        L.i("Message from " + message.from().username() + ": " + command.getCommand());
        if (command.execute(message, prefs)) {
            lastUserCommand.put(user, command);
        } else {
            telegramService.sendMessage(chatId, botService.getString(R.string.please_select_command), mainKeyboard);
        }
    }

    public Keyboard getMainKeyboard() {
        return mainKeyboard;
    }

    public ICommand findCommand(String text) {
        ICommand command = registeredCommandMap.get(text);
        if (command == null) {
            for (ICommand cmd : registeredCommandMap.values()) {
                if (cmd.getName().equals(text)) {
                    return cmd;
                }
            }
        }
        return command;
    }

    public Collection<ICommand> getRegisteredCommands() {
        return registeredCommandMap.values();
    }

    private <T extends AbstractCommand> T createInstance(Class<T> clazz) {
        try {
            Constructor constructor = clazz.getDeclaredConstructor(BotService.class);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(botService);
        } catch (Exception ex) {
            L.e(ex);
        }
        return null;
    }

}
