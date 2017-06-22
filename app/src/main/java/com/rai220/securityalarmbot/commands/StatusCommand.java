package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.BuildConfig;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.Converters;

import java.util.Set;

/**
 *
 */
public class StatusCommand extends AbstractCommand {

    public StatusCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/status";
    }

    @Override
    public String getName() {
        return "Status";
    }

    @Override
    public String getDescription() {
        return "Returns status of the telephone";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        Long chatId = message.chat().id();
        Prefs.UserPrefs userPrefs = prefs.getUser(message.from().id());
        if (userPrefs != null) {
            StringBuilder result = new StringBuilder();
            result.append(botService.getString(R.string.notifications_status)).append(userPrefs.isEventListener ?
                    botService.getString(R.string.on) : botService.getString(R.string.off));
            result.append(botService.getString(R.string.camera_mode_status)).append(prefs.cameraMode);

            boolean isAlive = botService.getObserver().isAlive();
            result.append(botService.getString(R.string.periodic_photo_status)).append(isAlive ?
                    botService.getString(R.string.on) : botService.getString(R.string.off));
            if (isAlive) {
                result.append(String.format(botService.getString(R.string.time_period_status), prefs.minutesPeriod));
            }

            result.append(botService.getString(R.string.md_status)).append(botService.getDetector().isAlive() ?
                    botService.getDetector().getMdType().toString() : botService.getString(R.string.off));
            result.append(String.format(botService.getString(R.string.sensitivity_status), prefs.sensitivity));

            result.append(botService.getString(R.string.shake_status)).append(botService.isSensorStarted() ?
                    botService.getString(R.string.on) : botService.getString(R.string.off));
            result.append(String.format(botService.getString(R.string.sensitivity_status), prefs.shakeSensitivity));
            result.append("\n");
            result.append(botService.getBatteryReceiver().getStatus());
            result.append("\n");
            Float temp = botService.getBatteryReceiver().getLastTemperature();
            if (temp != null) {
                result.append("\n").append(botService.getString(R.string.temperature_status)).append(temp);
            }

            result.append(botService.getString(R.string.register_user_status)).append(getUsersAsString(prefs.getUsers()));
            result.append(botService.getString(R.string.build_version_status)).append(BuildConfig.VERSION_NAME);
//            try {
//                String token = FirebaseInstanceId.getInstance().getToken();
//                result.append("\nFirebase token: ").append(token);
//            } catch (Throwable ex) {
//                L.e(ex);
//            }

            telegramService.sendMessage(chatId, result.toString());
        } else {
            telegramService.sendMessage(chatId, "Incorrect user!");
        }
        return false;
    }

    private String getUsersAsString(Set<Prefs.UserPrefs> users) {
        StringBuilder result = new StringBuilder();
        final String delimeter = ", ";
        for (Prefs.UserPrefs user : users) {
            result.append(Converters.USER_NAME_CONVERTER.apply(user))
                    .append(" (").append(user.isEventListener ? "on" : "off").append(")")
                    .append(delimeter);
        }
        result.deleteCharAt(result.length() - delimeter.length());
        return result.toString();
    }
}
