package com.rai220.securityalarmbot.commands;

import android.location.Location;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;

/**
 *
 */
public class GpsCommand extends AbstractCommand {

    public GpsCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/location";
    }

    @Override
    public String getName() {
        return "GPS";
    }

    @Override
    public String getDescription() {
        return "Get last GPS location";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        Location location = botService.getLocationController().getActual();
        Long chatId = message.chat().id();
        if (location != null) {
            telegramService.sendLocation(chatId, location);
            telegramService.notifyToOthers(message.from().id(), botService.getString(R.string.user_gps_location));
        } else {
            telegramService.sendMessage(chatId, "There is no last location. Turn on GPS.");
        }
        return false;
    }
}
