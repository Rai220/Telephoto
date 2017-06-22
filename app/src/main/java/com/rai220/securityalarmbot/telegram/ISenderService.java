package com.rai220.securityalarmbot.telegram;

import android.location.Location;

import com.pengrad.telegrambot.model.request.Keyboard;
import com.rai220.securityalarmbot.model.IncomingMessage;

import java.io.File;
import java.util.List;

/**
 *
 */

public interface ISenderService {
    void sendMessage(Long id, String message);
    void sendMessage(Long id, String message, Keyboard keyboard);
    void sendMessageToAll(String message);
    void sendMessageToAll(String message, Keyboard keyboard);
    void sendMessageToAll(List<IncomingMessage> incomingMessageList);

    void sendDocument(Long id, byte[] document, String fileName);

    void notifyToOthers(int userId, String message);

    void sendPhoto(Long id, byte[] photo);
    void sendPhoto(Long id, byte[] photo, String caption);
    void sendPhotoToAll(byte[] photo);
    void sendPhotoToAll(byte[] photo, String caption);

    void sendLocation(Long id, Location location);

    void sendAudio(Long chatId, File audio);

}
