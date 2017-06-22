package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */

public interface ICommand {

    /**
     * Returns unique command (starts with '/')
     *
     * @return command
     */
    String getCommand();

    /**
     * Returns name of command (using into button)
     *
     * @return short name of command
     */
    String getName();

    /**
     * Returns description of command
     *
     * @return long description about command (what to do)
     */
    String getDescription();

    /**
     * Enable or disable command function
     *
     * @return true if enabled
     */
    boolean isEnable();

    /**
     * Visible or hide command function
     *
     * @return true if hide
     */
    boolean isHide();

    /**
     * Execute command
     *
     * @param message incoming message
     * @param prefs
     * @return true if success
     */
    boolean execute(Message message, Prefs prefs);

    Collection<ICommand> execute(Message message);

}
