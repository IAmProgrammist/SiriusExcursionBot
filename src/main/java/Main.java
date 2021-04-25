import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendPhoto;
import text.TextLangs;
import text.TextManager;
import text.TextTypes;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import others.MySQLHelper;
import others.Places;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static TextManager textManager;
    private static TelegramBot bot;
    private static Map<Integer, ArrayList<Places>> visits = new HashMap<>();

    public static void init(String propFiles) {
        try {
            Logger.getLogger("Main").log(Level.INFO, "Started init...");
            Logger.getLogger("Main").log(Level.INFO, "Loading properties...");
            Properties properties = new Properties();
            properties.load(Main.class.getClassLoader().getResourceAsStream(propFiles));
            Logger.getLogger("Main").log(Level.INFO, "Loaded properties...");
            Logger.getLogger("Main").log(Level.INFO, "Loading locales...");
            textManager = new TextManager(properties.getProperty("localesName"));
            Logger.getLogger("Main").log(Level.INFO, "Loaded locales...");
            Logger.getLogger("Main").log(Level.INFO, "Loading database...");
            MySQLHelper.init(properties.getProperty("hostName"),
                    properties.getProperty("dbName"),
                    properties.getProperty("userName"),
                    properties.getProperty("password"));

            Logger.getLogger("Main").log(Level.INFO, "Reading from database...");
            visits = MySQLHelper.loadVisits();
            Logger.getLogger("Main").log(Level.INFO, "Loaded database...");
            Logger.getLogger("Main").log(Level.INFO, "Loading telegrambot...");
            bot = new TelegramBot(properties.getProperty("tokenId"));
            Logger.getLogger("Main").log(Level.INFO, "Loaded telegrambot...");
            Logger.getLogger("Main").log(Level.INFO, "Ready to work!");
        } catch (Exception e) {
            Logger.getLogger("Main").log(Level.INFO, "Init failed. Stacktrace:");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        init("config.properties");
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message().text() == null) {
                    String sendString = textManager.getLine(TextTypes.NO_TEXT, TextLangs.RU);
                    SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(), sendString));
                    continue;
                }
                TextLangs lang;
                if (update.message().from().languageCode().equalsIgnoreCase("ru")) {
                    lang = TextLangs.RU;
                } else {
                    lang = TextLangs.EN;
                }
                Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(
                        new String[]{textManager.getLine(TextTypes.ABOUT_TERRITORY_KB, lang),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_KB, lang),
                                textManager.getLine(TextTypes.OLYMP_PARK_KB, lang),
                                textManager.getLine(TextTypes.FISHT_KB, lang)},
                        new String[]{textManager.getLine(TextTypes.ICEBERG_KB, lang),
                                textManager.getLine(TextTypes.BIG_KB, lang),
                                textManager.getLine(TextTypes.TENNIS_ACADEMY_KB, lang),
                                textManager.getLine(TextTypes.F1_KB, lang)},
                        new String[]{textManager.getLine(TextTypes.IMMERTINSKY_KB, lang),
                                textManager.getLine(TextTypes.ZHD_KB, lang),
                                textManager.getLine(TextTypes.START_KB, lang)})
                        .resizeKeyboard(true)
                        .selective(true);
                ArrayList<Places> historyOfVisits = visits.getOrDefault(update.message().from().id(),
                        new ArrayList<Places>());
                if (update.message().text().equals("/start") || update.message().text()
                        .equals(textManager.getLine(TextTypes.START_KB, lang))) {
                    if (!historyOfVisits.contains(Places.START)) {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.GREETING_FIRST_START, lang));
                        sendString.append("\n");
                        sendString.append("\n");
                        for (Places place : Places.values()) {
                            if (place == Places.START) {
                                continue;
                            }
                            sendString.append(textManager.getLine(place, lang));
                            List<Places> places = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                            sendString.append(" ");
                            if (places.contains(place)) {
                                sendString.append(textManager.getLine(TextTypes.MET, lang));
                            } else {
                                sendString.append(textManager.getLine(TextTypes.NOMET, lang));
                            }
                            sendString.append("\n");
                            sendString.append("\n");
                        }
                        SendPhoto photo = null;
                        try {
                            URL j = Main.class.getClassLoader().getResource("images/top.jpg");
                            photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                            photo.caption(sendString.toString());
                            photo.replyMarkup(replyKeyboardMarkup);
                            bot.execute(photo);
                        } catch (Exception e) {
                            Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an " +
                                    "image. Stacktrace: ");
                            SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                    sendString.toString()).replyMarkup(replyKeyboardMarkup));
                            e.printStackTrace();
                        }
                        historyOfVisits.add(Places.START);
                        visits.put(update.message().from().id(), historyOfVisits);
                        try {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.START);
                        } catch (Exception throwables) {
                            Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing to " +
                                    "database. Stacktrace: ");
                            throwables.printStackTrace();
                        }
                    } else {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.GREETING_NOT_FIRST_START, lang));
                        sendString.append("\n");
                        sendString.append("\n");
                        for (Places place : Places.values()) {
                            if (place == Places.START) {
                                continue;
                            }
                            sendString.append(textManager.getLine(place, lang));
                            List<Places> places = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                            sendString.append(" ");
                            if (places.contains(place)) {
                                sendString.append(textManager.getLine(TextTypes.MET, lang));
                            } else {
                                sendString.append(textManager.getLine(TextTypes.NOMET, lang));
                            }
                            sendString.append("\n");

                            sendString.append("\n");
                        }
                        SendPhoto photo = null;
                        try {
                            URL j = Main.class.getClassLoader().getResource("images/top.jpg");
                            photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                            photo.caption(sendString.toString());
                            photo.replyMarkup(replyKeyboardMarkup);
                            bot.execute(photo);
                        } catch (Exception e) {
                            Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an " +
                                    "image. Stacktrace: ");
                            SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                    sendString.toString()).replyMarkup(replyKeyboardMarkup));
                            e.printStackTrace();
                        }
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.ABOUT_TERRITORY_KB, lang))) {
                    SendPhoto photo = null;
                    try {
                        URL j = Main.class.getClassLoader().getResource("images/territory.png");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(textManager.getLine(TextTypes.ABOUT_TERRITORY_DESC, lang));
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_TERRITORY_DESC, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }
                    try {
                        ArrayList<Places> pl = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                        if(!pl.contains(Places.ABOUT_TERRITORY)) {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.ABOUT_TERRITORY);
                            pl.add(Places.ABOUT_TERRITORY);
                            visits.put(update.message().from().id(), pl);
                        }
                    } catch (Exception throwables) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing visits. " +
                                "Stacktrace: ");
                        throwables.printStackTrace();
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.ABOUT_KAVKAZ_KB, lang))) {
                    SendPhoto photo = null;
                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC1, lang));
                        URL j = Main.class.getClassLoader().getResource("images/kavk1.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC11, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC1, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC11, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC2, lang));
                        URL j = Main.class.getClassLoader().getResource("images/kavk2.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC22, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC2, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC22, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC3, lang));
                        URL j = Main.class.getClassLoader().getResource("images/kavk3.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC33, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC3, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC33, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC4, lang));
                        URL j = Main.class.getClassLoader().getResource("images/kavk4.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC44, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC4, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.ABOUT_KAVKAZ_DESC44, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        ArrayList<Places> pl = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                        if(!pl.contains(Places.ABOUT_KAVKAZ)) {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.ABOUT_KAVKAZ);
                            pl.add(Places.ABOUT_KAVKAZ);
                            visits.put(update.message().from().id(), pl);
                        }
                    } catch (Exception throwables) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing visits. " +
                                "Stacktrace: ");
                        throwables.printStackTrace();
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.OLYMP_PARK_KB, lang))) {
                    SendPhoto photo = null;
                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.OLYMP_PARK_DESC1, lang));
                        URL j = Main.class.getClassLoader().getResource("images/olymp.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.OLYMP_PARK_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.OLYMP_PARK_DESC1, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.OLYMP_PARK_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        ArrayList<Places> pl = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                        if(!pl.contains(Places.OLYMP_PARK)) {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.OLYMP_PARK);
                            pl.add(Places.OLYMP_PARK);
                            visits.put(update.message().from().id(), pl);
                        }
                    } catch (Exception throwables) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing visits. " +
                                "Stacktrace: ");
                        throwables.printStackTrace();
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.FISHT_KB, lang))) {
                    SendPhoto photo = null;
                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.FISHT_DESC1, lang));
                        URL j = Main.class.getClassLoader().getResource("images/fisht.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.FISHT_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.FISHT_DESC1, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.FISHT_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        ArrayList<Places> pl = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                        if(!pl.contains(Places.FISHT)) {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.FISHT);
                            pl.add(Places.FISHT);
                            visits.put(update.message().from().id(), pl);
                        }
                    } catch (Exception throwables) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing visits. " +
                                "Stacktrace: ");
                        throwables.printStackTrace();
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.ICEBERG_KB, lang))) {
                    SendPhoto photo = null;
                    try {
                        StringBuilder sendString = new StringBuilder();
                        sendString.append(textManager.getLine(TextTypes.ICEBERG_DESC1, lang));
                        URL j = Main.class.getClassLoader().getResource("images/iceberg.jpg");
                        photo = new SendPhoto(update.message().chat().id(), new File(j.toURI()));
                        photo.caption(sendString.toString());
                        photo.replyMarkup(replyKeyboardMarkup);
                        bot.execute(photo);
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ICEBERG_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                    } catch (Exception e) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while sending an image. " +
                                "Stacktrace: ");
                        e.printStackTrace();
                        SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(),
                                textManager.getLine(TextTypes.ICEBERG_DESC1, lang) + "\n\n" +
                                        textManager.getLine(TextTypes.ICEBERG_DESC2, lang))
                                .replyMarkup(replyKeyboardMarkup));
                        e.printStackTrace();
                    }

                    try {
                        ArrayList<Places> pl = visits.getOrDefault(update.message().from().id(), new ArrayList<>());
                        if(!pl.contains(Places.ICEBERG)) {
                            MySQLHelper.writeVisit(update.message().from().id(), Places.ICEBERG);
                            pl.add(Places.ICEBERG);
                            visits.put(update.message().from().id(), pl);
                        }
                    } catch (Exception throwables) {
                        Logger.getLogger("Telegram Bot").log(Level.WARNING, "Got error while writing visits. " +
                                "Stacktrace: ");
                        throwables.printStackTrace();
                    }
                } else if (update.message().text().equals(textManager.getLine(TextTypes.BIG_KB, lang))) {

                } else if (update.message().text().equals(textManager.getLine(TextTypes.TENNIS_ACADEMY_KB, lang))) {

                } else if (update.message().text().equals(textManager.getLine(TextTypes.F1_KB, lang))) {

                } else if (update.message().text().equals(textManager.getLine(TextTypes.IMMERTINSKY_KB, lang))) {

                } else if (update.message().text().equals(textManager.getLine(TextTypes.ZHD_KB, lang))) {

                } else {
                    String sendString = textManager.getLine(TextTypes.UNRECOGNIZED, TextLangs.RU);
                    SendResponse toSend = bot.execute(new SendMessage(update.message().chat().id(), sendString).replyMarkup(replyKeyboardMarkup));
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}
