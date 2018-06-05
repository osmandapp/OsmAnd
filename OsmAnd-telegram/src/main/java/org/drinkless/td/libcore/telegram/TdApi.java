package org.drinkless.td.libcore.telegram;

import android.support.annotation.Nullable;
/**
 * This class contains as static nested classes all other TDLib interface
 * type-classes and function-classes.
 * <p>
 * It has no inner classes, functions or public members.
 */
public class TdApi {
    /**
     * This class is a base class for all TDLib interface classes.
     */
    public abstract static class Object {
        /**
         * @return string representation of the object.
         */
        public native String toString();

        /**
         * @return identifier uniquely determining type of the object.
         */
        public abstract int getConstructor();
    }

    /**
     * This class is a base class for all TDLib interface function-classes.
     */
    public abstract static class Function extends Object {
        /**
         * @return string representation of the object.
         */
        public native String toString();
    }

    /**
     * Contains information about the period of inactivity after which the current user's account will automatically be deleted.
     */
    public static class AccountTtl extends Object {
        /**
         * Number of days of inactivity before the account will be flagged for deletion; should range from 30-366 days.
         */
        public int days;

        /**
         * Default constructor.
         */
        public AccountTtl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param days Number of days of inactivity before the account will be flagged for deletion; should range from 30-366 days.
         */
        public AccountTtl(int days) {
            this.days = days;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1324495492;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1324495492;
        }
    }

    /**
     * Describes an animation file. The animation must be encoded in GIF or MPEG4 format.
     */
    public static class Animation extends Object {
        /**
         * Duration of the animation, in seconds; as defined by the sender.
         */
        public int duration;
        /**
         * Width of the animation.
         */
        public int width;
        /**
         * Height of the animation.
         */
        public int height;
        /**
         * Original name of the file; as defined by the sender.
         */
        public String fileName;
        /**
         * MIME type of the file, usually &quot;image/gif&quot; or &quot;video/mp4&quot;.
         */
        public String mimeType;
        /**
         * Animation thumbnail; may be null.
         */
        public @Nullable PhotoSize thumbnail;
        /**
         * File containing the animation.
         */
        public File animation;

        /**
         * Default constructor.
         */
        public Animation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param duration Duration of the animation, in seconds; as defined by the sender.
         * @param width Width of the animation.
         * @param height Height of the animation.
         * @param fileName Original name of the file; as defined by the sender.
         * @param mimeType MIME type of the file, usually &quot;image/gif&quot; or &quot;video/mp4&quot;.
         * @param thumbnail Animation thumbnail; may be null.
         * @param animation File containing the animation.
         */
        public Animation(int duration, int width, int height, String fileName, String mimeType, PhotoSize thumbnail, File animation) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.thumbnail = thumbnail;
            this.animation = animation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1723168340;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1723168340;
        }
    }

    /**
     * Represents a list of animations.
     */
    public static class Animations extends Object {
        /**
         * List of animations.
         */
        public Animation[] animations;

        /**
         * Default constructor.
         */
        public Animations() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animations List of animations.
         */
        public Animations(Animation[] animations) {
            this.animations = animations;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 344216945;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 344216945;
        }
    }

    /**
     * Describes an audio file. Audio is usually in MP3 format.
     */
    public static class Audio extends Object {
        /**
         * Duration of the audio, in seconds; as defined by the sender.
         */
        public int duration;
        /**
         * Title of the audio; as defined by the sender.
         */
        public String title;
        /**
         * Performer of the audio; as defined by the sender.
         */
        public String performer;
        /**
         * Original name of the file; as defined by the sender.
         */
        public String fileName;
        /**
         * The MIME type of the file; as defined by the sender.
         */
        public String mimeType;
        /**
         * The thumbnail of the album cover; as defined by the sender. The full size thumbnail should be extracted from the downloaded file; may be null.
         */
        public @Nullable PhotoSize albumCoverThumbnail;
        /**
         * File containing the audio.
         */
        public File audio;

        /**
         * Default constructor.
         */
        public Audio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param duration Duration of the audio, in seconds; as defined by the sender.
         * @param title Title of the audio; as defined by the sender.
         * @param performer Performer of the audio; as defined by the sender.
         * @param fileName Original name of the file; as defined by the sender.
         * @param mimeType The MIME type of the file; as defined by the sender.
         * @param albumCoverThumbnail The thumbnail of the album cover; as defined by the sender. The full size thumbnail should be extracted from the downloaded file; may be null.
         * @param audio File containing the audio.
         */
        public Audio(int duration, String title, String performer, String fileName, String mimeType, PhotoSize albumCoverThumbnail, File audio) {
            this.duration = duration;
            this.title = title;
            this.performer = performer;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.albumCoverThumbnail = albumCoverThumbnail;
            this.audio = audio;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 383148432;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 383148432;
        }
    }

    /**
     * Information about the authentication code that was sent.
     */
    public static class AuthenticationCodeInfo extends Object {
        /**
         * A phone number that is being authenticated.
         */
        public String phoneNumber;
        /**
         * Describes the way the code was sent to the user.
         */
        public AuthenticationCodeType type;
        /**
         * Describes the way the next code will be sent to the user; may be null.
         */
        public @Nullable AuthenticationCodeType nextType;
        /**
         * Timeout before the code should be re-sent, in seconds.
         */
        public int timeout;

        /**
         * Default constructor.
         */
        public AuthenticationCodeInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param phoneNumber A phone number that is being authenticated.
         * @param type Describes the way the code was sent to the user.
         * @param nextType Describes the way the next code will be sent to the user; may be null.
         * @param timeout Timeout before the code should be re-sent, in seconds.
         */
        public AuthenticationCodeInfo(String phoneNumber, AuthenticationCodeType type, AuthenticationCodeType nextType, int timeout) {
            this.phoneNumber = phoneNumber;
            this.type = type;
            this.nextType = nextType;
            this.timeout = timeout;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -860345416;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -860345416;
        }
    }

    /**
     * This class is an abstract base class.
     * Provides information about the method by which an authentication code is delivered to the user.
     */
    public abstract static class AuthenticationCodeType extends Object {
    }

    /**
     * An authentication code is delivered via a private Telegram message, which can be viewed in another client.
     */
    public static class AuthenticationCodeTypeTelegramMessage extends AuthenticationCodeType {
        /**
         * Length of the code.
         */
        public int length;

        /**
         * Default constructor.
         */
        public AuthenticationCodeTypeTelegramMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param length Length of the code.
         */
        public AuthenticationCodeTypeTelegramMessage(int length) {
            this.length = length;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2079628074;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2079628074;
        }
    }

    /**
     * An authentication code is delivered via an SMS message to the specified phone number.
     */
    public static class AuthenticationCodeTypeSms extends AuthenticationCodeType {
        /**
         * Length of the code.
         */
        public int length;

        /**
         * Default constructor.
         */
        public AuthenticationCodeTypeSms() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param length Length of the code.
         */
        public AuthenticationCodeTypeSms(int length) {
            this.length = length;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 962650760;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 962650760;
        }
    }

    /**
     * An authentication code is delivered via a phone call to the specified phone number.
     */
    public static class AuthenticationCodeTypeCall extends AuthenticationCodeType {
        /**
         * Length of the code.
         */
        public int length;

        /**
         * Default constructor.
         */
        public AuthenticationCodeTypeCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param length Length of the code.
         */
        public AuthenticationCodeTypeCall(int length) {
            this.length = length;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1636265063;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1636265063;
        }
    }

    /**
     * An authentication code is delivered by an immediately cancelled call to the specified phone number. The number from which the call was made is the code.
     */
    public static class AuthenticationCodeTypeFlashCall extends AuthenticationCodeType {
        /**
         * Pattern of the phone number from which the call will be made.
         */
        public String pattern;

        /**
         * Default constructor.
         */
        public AuthenticationCodeTypeFlashCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pattern Pattern of the phone number from which the call will be made.
         */
        public AuthenticationCodeTypeFlashCall(String pattern) {
            this.pattern = pattern;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1395882402;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1395882402;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the current authorization state of the client.
     */
    public abstract static class AuthorizationState extends Object {
    }

    /**
     * TDLib needs TdlibParameters for initialization.
     */
    public static class AuthorizationStateWaitTdlibParameters extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateWaitTdlibParameters() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 904720988;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 904720988;
        }
    }

    /**
     * TDLib needs an encryption key to decrypt the local database.
     */
    public static class AuthorizationStateWaitEncryptionKey extends AuthorizationState {
        /**
         * True, if the database is currently encrypted.
         */
        public boolean isEncrypted;

        /**
         * Default constructor.
         */
        public AuthorizationStateWaitEncryptionKey() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isEncrypted True, if the database is currently encrypted.
         */
        public AuthorizationStateWaitEncryptionKey(boolean isEncrypted) {
            this.isEncrypted = isEncrypted;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 612103496;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 612103496;
        }
    }

    /**
     * TDLib needs the user's phone number to authorize.
     */
    public static class AuthorizationStateWaitPhoneNumber extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateWaitPhoneNumber() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 306402531;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 306402531;
        }
    }

    /**
     * TDLib needs the user's authentication code to finalize authorization.
     */
    public static class AuthorizationStateWaitCode extends AuthorizationState {
        /**
         * True, if the user is already registered.
         */
        public boolean isRegistered;
        /**
         * Information about the authorization code that was sent.
         */
        public AuthenticationCodeInfo codeInfo;

        /**
         * Default constructor.
         */
        public AuthorizationStateWaitCode() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isRegistered True, if the user is already registered.
         * @param codeInfo Information about the authorization code that was sent.
         */
        public AuthorizationStateWaitCode(boolean isRegistered, AuthenticationCodeInfo codeInfo) {
            this.isRegistered = isRegistered;
            this.codeInfo = codeInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -483510157;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -483510157;
        }
    }

    /**
     * The user has been authorized, but needs to enter a password to start using the application.
     */
    public static class AuthorizationStateWaitPassword extends AuthorizationState {
        /**
         * Hint for the password; can be empty.
         */
        public String passwordHint;
        /**
         * True if a recovery email address has been set up.
         */
        public boolean hasRecoveryEmailAddress;
        /**
         * Pattern of the email address to which the recovery email was sent; empty until a recovery email has been sent.
         */
        public String recoveryEmailAddressPattern;

        /**
         * Default constructor.
         */
        public AuthorizationStateWaitPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param passwordHint Hint for the password; can be empty.
         * @param hasRecoveryEmailAddress True if a recovery email address has been set up.
         * @param recoveryEmailAddressPattern Pattern of the email address to which the recovery email was sent; empty until a recovery email has been sent.
         */
        public AuthorizationStateWaitPassword(String passwordHint, boolean hasRecoveryEmailAddress, String recoveryEmailAddressPattern) {
            this.passwordHint = passwordHint;
            this.hasRecoveryEmailAddress = hasRecoveryEmailAddress;
            this.recoveryEmailAddressPattern = recoveryEmailAddressPattern;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 187548796;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 187548796;
        }
    }

    /**
     * The user has been successfully authorized. TDLib is now ready to answer queries.
     */
    public static class AuthorizationStateReady extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateReady() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1834871737;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1834871737;
        }
    }

    /**
     * The user is currently logging out.
     */
    public static class AuthorizationStateLoggingOut extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateLoggingOut() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 154449270;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 154449270;
        }
    }

    /**
     * TDLib is closing, all subsequent queries will be answered with the error 500. Note that closing TDLib can take a while. All resources will be freed only after authorizationStateClosed has been received.
     */
    public static class AuthorizationStateClosing extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateClosing() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 445855311;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 445855311;
        }
    }

    /**
     * TDLib client is in its final state. All databases are closed and all resources are released. No other updates will be received after this. All queries will be responded to with error code 500. To continue working, one should create a new instance of the TDLib client.
     */
    public static class AuthorizationStateClosed extends AuthorizationState {

        /**
         * Default constructor.
         */
        public AuthorizationStateClosed() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1526047584;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1526047584;
        }
    }

    /**
     * Represents a basic group of 0-200 users (must be upgraded to a supergroup to accommodate more than 200 users).
     */
    public static class BasicGroup extends Object {
        /**
         * Group identifier.
         */
        public int id;
        /**
         * Number of members in the group.
         */
        public int memberCount;
        /**
         * Status of the current user in the group.
         */
        public ChatMemberStatus status;
        /**
         * True, if all members have been granted administrator rights in the group.
         */
        public boolean everyoneIsAdministrator;
        /**
         * True, if the group is active.
         */
        public boolean isActive;
        /**
         * Identifier of the supergroup to which this group was upgraded; 0 if none.
         */
        public int upgradedToSupergroupId;

        /**
         * Default constructor.
         */
        public BasicGroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Group identifier.
         * @param memberCount Number of members in the group.
         * @param status Status of the current user in the group.
         * @param everyoneIsAdministrator True, if all members have been granted administrator rights in the group.
         * @param isActive True, if the group is active.
         * @param upgradedToSupergroupId Identifier of the supergroup to which this group was upgraded; 0 if none.
         */
        public BasicGroup(int id, int memberCount, ChatMemberStatus status, boolean everyoneIsAdministrator, boolean isActive, int upgradedToSupergroupId) {
            this.id = id;
            this.memberCount = memberCount;
            this.status = status;
            this.everyoneIsAdministrator = everyoneIsAdministrator;
            this.isActive = isActive;
            this.upgradedToSupergroupId = upgradedToSupergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1572712718;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1572712718;
        }
    }

    /**
     * Contains full information about a basic group.
     */
    public static class BasicGroupFullInfo extends Object {
        /**
         * User identifier of the creator of the group; 0 if unknown.
         */
        public int creatorUserId;
        /**
         * Group members.
         */
        public ChatMember[] members;
        /**
         * Invite link for this group; available only for the group creator and only after it has been generated at least once.
         */
        public String inviteLink;

        /**
         * Default constructor.
         */
        public BasicGroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param creatorUserId User identifier of the creator of the group; 0 if unknown.
         * @param members Group members.
         * @param inviteLink Invite link for this group; available only for the group creator and only after it has been generated at least once.
         */
        public BasicGroupFullInfo(int creatorUserId, ChatMember[] members, String inviteLink) {
            this.creatorUserId = creatorUserId;
            this.members = members;
            this.inviteLink = inviteLink;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 952266076;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 952266076;
        }
    }

    /**
     * Represents commands supported by a bot.
     */
    public static class BotCommand extends Object {
        /**
         * Text of the bot command.
         */
        public String command;
        /**
         * Description of the bot command.
         */
        public String description;

        /**
         * Default constructor.
         */
        public BotCommand() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param command Text of the bot command.
         * @param description Description of the bot command.
         */
        public BotCommand(String command, String description) {
            this.command = command;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1032140601;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1032140601;
        }
    }

    /**
     * Provides information about a bot and its supported commands.
     */
    public static class BotInfo extends Object {
        /**
         * Long description shown on the user info page.
         */
        public String description;
        /**
         * A list of commands supported by the bot.
         */
        public BotCommand[] commands;

        /**
         * Default constructor.
         */
        public BotInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param description Long description shown on the user info page.
         * @param commands A list of commands supported by the bot.
         */
        public BotInfo(String description, BotCommand[] commands) {
            this.description = description;
            this.commands = commands;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1296528907;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1296528907;
        }
    }

    /**
     * Describes a call.
     */
    public static class Call extends Object {
        /**
         * Call identifier, not persistent.
         */
        public int id;
        /**
         * Peer user identifier.
         */
        public int userId;
        /**
         * True, if the call is outgoing.
         */
        public boolean isOutgoing;
        /**
         * Call state.
         */
        public CallState state;

        /**
         * Default constructor.
         */
        public Call() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Call identifier, not persistent.
         * @param userId Peer user identifier.
         * @param isOutgoing True, if the call is outgoing.
         * @param state Call state.
         */
        public Call(int id, int userId, boolean isOutgoing, CallState state) {
            this.id = id;
            this.userId = userId;
            this.isOutgoing = isOutgoing;
            this.state = state;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1837599107;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1837599107;
        }
    }

    /**
     * Describes the address of UDP reflectors.
     */
    public static class CallConnection extends Object {
        /**
         * Reflector identifier.
         */
        public long id;
        /**
         * IPv4 reflector address.
         */
        public String ip;
        /**
         * IPv6 reflector address.
         */
        public String ipv6;
        /**
         * Reflector port number.
         */
        public int port;
        /**
         * Connection peer tag.
         */
        public byte[] peerTag;

        /**
         * Default constructor.
         */
        public CallConnection() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Reflector identifier.
         * @param ip IPv4 reflector address.
         * @param ipv6 IPv6 reflector address.
         * @param port Reflector port number.
         * @param peerTag Connection peer tag.
         */
        public CallConnection(long id, String ip, String ipv6, int port, byte[] peerTag) {
            this.id = id;
            this.ip = ip;
            this.ipv6 = ipv6;
            this.port = port;
            this.peerTag = peerTag;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1318542714;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1318542714;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the reason why a call was discarded.
     */
    public abstract static class CallDiscardReason extends Object {
    }

    /**
     * The call wasn't discarded, or the reason is unknown.
     */
    public static class CallDiscardReasonEmpty extends CallDiscardReason {

        /**
         * Default constructor.
         */
        public CallDiscardReasonEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1258917949;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1258917949;
        }
    }

    /**
     * The call was ended before the conversation started. It was cancelled by the caller or missed by the other party.
     */
    public static class CallDiscardReasonMissed extends CallDiscardReason {

        /**
         * Default constructor.
         */
        public CallDiscardReasonMissed() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1680358012;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1680358012;
        }
    }

    /**
     * The call was ended before the conversation started. It was declined by the other party.
     */
    public static class CallDiscardReasonDeclined extends CallDiscardReason {

        /**
         * Default constructor.
         */
        public CallDiscardReasonDeclined() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1729926094;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1729926094;
        }
    }

    /**
     * The call was ended during the conversation because the users were disconnected.
     */
    public static class CallDiscardReasonDisconnected extends CallDiscardReason {

        /**
         * Default constructor.
         */
        public CallDiscardReasonDisconnected() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1342872670;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1342872670;
        }
    }

    /**
     * The call was ended because one of the parties hung up.
     */
    public static class CallDiscardReasonHungUp extends CallDiscardReason {

        /**
         * Default constructor.
         */
        public CallDiscardReasonHungUp() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 438216166;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 438216166;
        }
    }

    /**
     * Contains the call identifier.
     */
    public static class CallId extends Object {
        /**
         * Call identifier.
         */
        public int id;

        /**
         * Default constructor.
         */
        public CallId() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Call identifier.
         */
        public CallId(int id) {
            this.id = id;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 65717769;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 65717769;
        }
    }

    /**
     * Specifies the supported call protocols.
     */
    public static class CallProtocol extends Object {
        /**
         * True, if UDP peer-to-peer connections are supported.
         */
        public boolean udpP2p;
        /**
         * True, if connection through UDP reflectors is supported.
         */
        public boolean udpReflector;
        /**
         * Minimum supported API layer; use 65.
         */
        public int minLayer;
        /**
         * Maximum supported API layer; use 65.
         */
        public int maxLayer;

        /**
         * Default constructor.
         */
        public CallProtocol() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param udpP2p True, if UDP peer-to-peer connections are supported.
         * @param udpReflector True, if connection through UDP reflectors is supported.
         * @param minLayer Minimum supported API layer; use 65.
         * @param maxLayer Maximum supported API layer; use 65.
         */
        public CallProtocol(boolean udpP2p, boolean udpReflector, int minLayer, int maxLayer) {
            this.udpP2p = udpP2p;
            this.udpReflector = udpReflector;
            this.minLayer = minLayer;
            this.maxLayer = maxLayer;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1042830667;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1042830667;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the current call state.
     */
    public abstract static class CallState extends Object {
    }

    /**
     * The call is pending, waiting to be accepted by a user.
     */
    public static class CallStatePending extends CallState {
        /**
         * True, if the call has already been created by the server.
         */
        public boolean isCreated;
        /**
         * True, if the call has already been received by the other party.
         */
        public boolean isReceived;

        /**
         * Default constructor.
         */
        public CallStatePending() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isCreated True, if the call has already been created by the server.
         * @param isReceived True, if the call has already been received by the other party.
         */
        public CallStatePending(boolean isCreated, boolean isReceived) {
            this.isCreated = isCreated;
            this.isReceived = isReceived;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1073048620;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1073048620;
        }
    }

    /**
     * The call has been answered and encryption keys are being exchanged.
     */
    public static class CallStateExchangingKeys extends CallState {

        /**
         * Default constructor.
         */
        public CallStateExchangingKeys() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1848149403;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1848149403;
        }
    }

    /**
     * The call is ready to use.
     */
    public static class CallStateReady extends CallState {
        /**
         * Call protocols supported by the peer.
         */
        public CallProtocol protocol;
        /**
         * Available UDP reflectors.
         */
        public CallConnection[] connections;
        /**
         * A JSON-encoded call config.
         */
        public String config;
        /**
         * Call encryption key.
         */
        public byte[] encryptionKey;
        /**
         * Encryption key emojis fingerprint.
         */
        public String[] emojis;

        /**
         * Default constructor.
         */
        public CallStateReady() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param protocol Call protocols supported by the peer.
         * @param connections Available UDP reflectors.
         * @param config A JSON-encoded call config.
         * @param encryptionKey Call encryption key.
         * @param emojis Encryption key emojis fingerprint.
         */
        public CallStateReady(CallProtocol protocol, CallConnection[] connections, String config, byte[] encryptionKey, String[] emojis) {
            this.protocol = protocol;
            this.connections = connections;
            this.config = config;
            this.encryptionKey = encryptionKey;
            this.emojis = emojis;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1518705438;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1518705438;
        }
    }

    /**
     * The call is hanging up after discardCall has been called.
     */
    public static class CallStateHangingUp extends CallState {

        /**
         * Default constructor.
         */
        public CallStateHangingUp() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2133790038;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2133790038;
        }
    }

    /**
     * The call has ended successfully.
     */
    public static class CallStateDiscarded extends CallState {
        /**
         * The reason, why the call has ended.
         */
        public CallDiscardReason reason;
        /**
         * True, if the call rating should be sent to the server.
         */
        public boolean needRating;
        /**
         * True, if the call debug information should be sent to the server.
         */
        public boolean needDebugInformation;

        /**
         * Default constructor.
         */
        public CallStateDiscarded() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param reason The reason, why the call has ended.
         * @param needRating True, if the call rating should be sent to the server.
         * @param needDebugInformation True, if the call debug information should be sent to the server.
         */
        public CallStateDiscarded(CallDiscardReason reason, boolean needRating, boolean needDebugInformation) {
            this.reason = reason;
            this.needRating = needRating;
            this.needDebugInformation = needDebugInformation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -190853167;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -190853167;
        }
    }

    /**
     * The call has ended with an error.
     */
    public static class CallStateError extends CallState {
        /**
         * Error. An error with the code 4005000 will be returned if an outgoing call is missed because of an expired timeout.
         */
        public Error error;

        /**
         * Default constructor.
         */
        public CallStateError() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param error Error. An error with the code 4005000 will be returned if an outgoing call is missed because of an expired timeout.
         */
        public CallStateError(Error error) {
            this.error = error;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -975215467;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -975215467;
        }
    }

    /**
     * Contains a bot's answer to a callback query.
     */
    public static class CallbackQueryAnswer extends Object {
        /**
         * Text of the answer.
         */
        public String text;
        /**
         * True, if an alert should be shown to the user instead of a toast notification.
         */
        public boolean showAlert;
        /**
         * URL to be opened.
         */
        public String url;

        /**
         * Default constructor.
         */
        public CallbackQueryAnswer() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text of the answer.
         * @param showAlert True, if an alert should be shown to the user instead of a toast notification.
         * @param url URL to be opened.
         */
        public CallbackQueryAnswer(String text, boolean showAlert, String url) {
            this.text = text;
            this.showAlert = showAlert;
            this.url = url;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 360867933;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 360867933;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a payload of a callback query.
     */
    public abstract static class CallbackQueryPayload extends Object {
    }

    /**
     * The payload from a general callback button.
     */
    public static class CallbackQueryPayloadData extends CallbackQueryPayload {
        /**
         * Data that was attached to the callback button.
         */
        public byte[] data;

        /**
         * Default constructor.
         */
        public CallbackQueryPayloadData() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param data Data that was attached to the callback button.
         */
        public CallbackQueryPayloadData(byte[] data) {
            this.data = data;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1977729946;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1977729946;
        }
    }

    /**
     * The payload from a game callback button.
     */
    public static class CallbackQueryPayloadGame extends CallbackQueryPayload {
        /**
         * A short name of the game that was attached to the callback button.
         */
        public String gameShortName;

        /**
         * Default constructor.
         */
        public CallbackQueryPayloadGame() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param gameShortName A short name of the game that was attached to the callback button.
         */
        public CallbackQueryPayloadGame(String gameShortName) {
            this.gameShortName = gameShortName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1303571512;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1303571512;
        }
    }

    /**
     * A chat. (Can be a private chat, basic group, supergroup, or secret chat.)
     */
    public static class Chat extends Object {
        /**
         * Chat unique identifier.
         */
        public long id;
        /**
         * Type of the chat.
         */
        public ChatType type;
        /**
         * Chat title.
         */
        public String title;
        /**
         * Chat photo; may be null.
         */
        public @Nullable ChatPhoto photo;
        /**
         * Last message in the chat; may be null.
         */
        public @Nullable Message lastMessage;
        /**
         * Descending parameter by which chats are sorted in the main chat list. If the order number of two chats is the same, they must be sorted in descending order by ID. If 0, the position of the chat in the list is undetermined.
         */
        public long order;
        /**
         * True, if the chat is pinned.
         */
        public boolean isPinned;
        /**
         * True, if the chat can be reported to Telegram moderators through reportChat.
         */
        public boolean canBeReported;
        /**
         * Number of unread messages in the chat.
         */
        public int unreadCount;
        /**
         * Identifier of the last read incoming message.
         */
        public long lastReadInboxMessageId;
        /**
         * Identifier of the last read outgoing message.
         */
        public long lastReadOutboxMessageId;
        /**
         * Number of unread messages with a mention/reply in the chat.
         */
        public int unreadMentionCount;
        /**
         * Notification settings for this chat.
         */
        public NotificationSettings notificationSettings;
        /**
         * Identifier of the message from which reply markup needs to be used; 0 if there is no default custom reply markup in the chat.
         */
        public long replyMarkupMessageId;
        /**
         * A draft of a message in the chat; may be null.
         */
        public @Nullable DraftMessage draftMessage;
        /**
         * Contains client-specific data associated with the chat. (For example, the chat position or local chat notification settings can be stored here.) Persistent if a message database is used.
         */
        public String clientData;

        /**
         * Default constructor.
         */
        public Chat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Chat unique identifier.
         * @param type Type of the chat.
         * @param title Chat title.
         * @param photo Chat photo; may be null.
         * @param lastMessage Last message in the chat; may be null.
         * @param order Descending parameter by which chats are sorted in the main chat list. If the order number of two chats is the same, they must be sorted in descending order by ID. If 0, the position of the chat in the list is undetermined.
         * @param isPinned True, if the chat is pinned.
         * @param canBeReported True, if the chat can be reported to Telegram moderators through reportChat.
         * @param unreadCount Number of unread messages in the chat.
         * @param lastReadInboxMessageId Identifier of the last read incoming message.
         * @param lastReadOutboxMessageId Identifier of the last read outgoing message.
         * @param unreadMentionCount Number of unread messages with a mention/reply in the chat.
         * @param notificationSettings Notification settings for this chat.
         * @param replyMarkupMessageId Identifier of the message from which reply markup needs to be used; 0 if there is no default custom reply markup in the chat.
         * @param draftMessage A draft of a message in the chat; may be null.
         * @param clientData Contains client-specific data associated with the chat. (For example, the chat position or local chat notification settings can be stored here.) Persistent if a message database is used.
         */
        public Chat(long id, ChatType type, String title, ChatPhoto photo, Message lastMessage, long order, boolean isPinned, boolean canBeReported, int unreadCount, long lastReadInboxMessageId, long lastReadOutboxMessageId, int unreadMentionCount, NotificationSettings notificationSettings, long replyMarkupMessageId, DraftMessage draftMessage, String clientData) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.photo = photo;
            this.lastMessage = lastMessage;
            this.order = order;
            this.isPinned = isPinned;
            this.canBeReported = canBeReported;
            this.unreadCount = unreadCount;
            this.lastReadInboxMessageId = lastReadInboxMessageId;
            this.lastReadOutboxMessageId = lastReadOutboxMessageId;
            this.unreadMentionCount = unreadMentionCount;
            this.notificationSettings = notificationSettings;
            this.replyMarkupMessageId = replyMarkupMessageId;
            this.draftMessage = draftMessage;
            this.clientData = clientData;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1599984597;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1599984597;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the different types of activity in a chat.
     */
    public abstract static class ChatAction extends Object {
    }

    /**
     * The user is typing a message.
     */
    public static class ChatActionTyping extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionTyping() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 380122167;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 380122167;
        }
    }

    /**
     * The user is recording a video.
     */
    public static class ChatActionRecordingVideo extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionRecordingVideo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 216553362;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 216553362;
        }
    }

    /**
     * The user is uploading a video.
     */
    public static class ChatActionUploadingVideo extends ChatAction {
        /**
         * Upload progress, as a percentage.
         */
        public int progress;

        /**
         * Default constructor.
         */
        public ChatActionUploadingVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param progress Upload progress, as a percentage.
         */
        public ChatActionUploadingVideo(int progress) {
            this.progress = progress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1234185270;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1234185270;
        }
    }

    /**
     * The user is recording a voice note.
     */
    public static class ChatActionRecordingVoiceNote extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionRecordingVoiceNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -808850058;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -808850058;
        }
    }

    /**
     * The user is uploading a voice note.
     */
    public static class ChatActionUploadingVoiceNote extends ChatAction {
        /**
         * Upload progress, as a percentage.
         */
        public int progress;

        /**
         * Default constructor.
         */
        public ChatActionUploadingVoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param progress Upload progress, as a percentage.
         */
        public ChatActionUploadingVoiceNote(int progress) {
            this.progress = progress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -613643666;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -613643666;
        }
    }

    /**
     * The user is uploading a photo.
     */
    public static class ChatActionUploadingPhoto extends ChatAction {
        /**
         * Upload progress, as a percentage.
         */
        public int progress;

        /**
         * Default constructor.
         */
        public ChatActionUploadingPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param progress Upload progress, as a percentage.
         */
        public ChatActionUploadingPhoto(int progress) {
            this.progress = progress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 654240583;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 654240583;
        }
    }

    /**
     * The user is uploading a document.
     */
    public static class ChatActionUploadingDocument extends ChatAction {
        /**
         * Upload progress, as a percentage.
         */
        public int progress;

        /**
         * Default constructor.
         */
        public ChatActionUploadingDocument() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param progress Upload progress, as a percentage.
         */
        public ChatActionUploadingDocument(int progress) {
            this.progress = progress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 167884362;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 167884362;
        }
    }

    /**
     * The user is picking a location or venue to send.
     */
    public static class ChatActionChoosingLocation extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionChoosingLocation() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2017893596;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2017893596;
        }
    }

    /**
     * The user is picking a contact to send.
     */
    public static class ChatActionChoosingContact extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionChoosingContact() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1222507496;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1222507496;
        }
    }

    /**
     * The user has started to play a game.
     */
    public static class ChatActionStartPlayingGame extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionStartPlayingGame() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -865884164;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -865884164;
        }
    }

    /**
     * The user is recording a video note.
     */
    public static class ChatActionRecordingVideoNote extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionRecordingVideoNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 16523393;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 16523393;
        }
    }

    /**
     * The user is uploading a video note.
     */
    public static class ChatActionUploadingVideoNote extends ChatAction {
        /**
         * Upload progress, as a percentage.
         */
        public int progress;

        /**
         * Default constructor.
         */
        public ChatActionUploadingVideoNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param progress Upload progress, as a percentage.
         */
        public ChatActionUploadingVideoNote(int progress) {
            this.progress = progress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1172364918;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1172364918;
        }
    }

    /**
     * The user has cancelled the previous action.
     */
    public static class ChatActionCancel extends ChatAction {

        /**
         * Default constructor.
         */
        public ChatActionCancel() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1160523958;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1160523958;
        }
    }

    /**
     * Represents a chat event.
     */
    public static class ChatEvent extends Object {
        /**
         * Chat event identifier.
         */
        public long id;
        /**
         * Point in time (Unix timestamp) when the event happened.
         */
        public int date;
        /**
         * Identifier of the user who performed the action that triggered the event.
         */
        public int userId;
        /**
         * Action performed by the user.
         */
        public ChatEventAction action;

        /**
         * Default constructor.
         */
        public ChatEvent() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Chat event identifier.
         * @param date Point in time (Unix timestamp) when the event happened.
         * @param userId Identifier of the user who performed the action that triggered the event.
         * @param action Action performed by the user.
         */
        public ChatEvent(long id, int date, int userId, ChatEventAction action) {
            this.id = id;
            this.date = date;
            this.userId = userId;
            this.action = action;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -609912404;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -609912404;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a chat event.
     */
    public abstract static class ChatEventAction extends Object {
    }

    /**
     * A message was edited.
     */
    public static class ChatEventMessageEdited extends ChatEventAction {
        /**
         * The original message before the edit.
         */
        public Message oldMessage;
        /**
         * The message after it was edited.
         */
        public Message newMessage;

        /**
         * Default constructor.
         */
        public ChatEventMessageEdited() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldMessage The original message before the edit.
         * @param newMessage The message after it was edited.
         */
        public ChatEventMessageEdited(Message oldMessage, Message newMessage) {
            this.oldMessage = oldMessage;
            this.newMessage = newMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -430967304;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -430967304;
        }
    }

    /**
     * A message was deleted.
     */
    public static class ChatEventMessageDeleted extends ChatEventAction {
        /**
         * Deleted message.
         */
        public Message message;

        /**
         * Default constructor.
         */
        public ChatEventMessageDeleted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param message Deleted message.
         */
        public ChatEventMessageDeleted(Message message) {
            this.message = message;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -892974601;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -892974601;
        }
    }

    /**
     * A message was pinned.
     */
    public static class ChatEventMessagePinned extends ChatEventAction {
        /**
         * Pinned message.
         */
        public Message message;

        /**
         * Default constructor.
         */
        public ChatEventMessagePinned() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param message Pinned message.
         */
        public ChatEventMessagePinned(Message message) {
            this.message = message;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 438742298;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 438742298;
        }
    }

    /**
     * A message was unpinned.
     */
    public static class ChatEventMessageUnpinned extends ChatEventAction {

        /**
         * Default constructor.
         */
        public ChatEventMessageUnpinned() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2002594849;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2002594849;
        }
    }

    /**
     * A new member joined the chat.
     */
    public static class ChatEventMemberJoined extends ChatEventAction {

        /**
         * Default constructor.
         */
        public ChatEventMemberJoined() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -235468508;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -235468508;
        }
    }

    /**
     * A member left the chat.
     */
    public static class ChatEventMemberLeft extends ChatEventAction {

        /**
         * Default constructor.
         */
        public ChatEventMemberLeft() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -948420593;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -948420593;
        }
    }

    /**
     * A new chat member was invited.
     */
    public static class ChatEventMemberInvited extends ChatEventAction {
        /**
         * New member user identifier.
         */
        public int userId;
        /**
         * New member status.
         */
        public ChatMemberStatus status;

        /**
         * Default constructor.
         */
        public ChatEventMemberInvited() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId New member user identifier.
         * @param status New member status.
         */
        public ChatEventMemberInvited(int userId, ChatMemberStatus status) {
            this.userId = userId;
            this.status = status;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2093688706;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2093688706;
        }
    }

    /**
     * A chat member has gained/lost administrator status, or the list of their administrator privileges has changed.
     */
    public static class ChatEventMemberPromoted extends ChatEventAction {
        /**
         * Chat member user identifier.
         */
        public int userId;
        /**
         * Previous status of the chat member.
         */
        public ChatMemberStatus oldStatus;
        /**
         * New status of the chat member.
         */
        public ChatMemberStatus newStatus;

        /**
         * Default constructor.
         */
        public ChatEventMemberPromoted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Chat member user identifier.
         * @param oldStatus Previous status of the chat member.
         * @param newStatus New status of the chat member.
         */
        public ChatEventMemberPromoted(int userId, ChatMemberStatus oldStatus, ChatMemberStatus newStatus) {
            this.userId = userId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1887176186;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1887176186;
        }
    }

    /**
     * A chat member was restricted/unrestricted or banned/unbanned, or the list of their restrictions has changed.
     */
    public static class ChatEventMemberRestricted extends ChatEventAction {
        /**
         * Chat member user identifier.
         */
        public int userId;
        /**
         * Previous status of the chat member.
         */
        public ChatMemberStatus oldStatus;
        /**
         * New status of the chat member.
         */
        public ChatMemberStatus newStatus;

        /**
         * Default constructor.
         */
        public ChatEventMemberRestricted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Chat member user identifier.
         * @param oldStatus Previous status of the chat member.
         * @param newStatus New status of the chat member.
         */
        public ChatEventMemberRestricted(int userId, ChatMemberStatus oldStatus, ChatMemberStatus newStatus) {
            this.userId = userId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 584946294;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 584946294;
        }
    }

    /**
     * The chat title was changed.
     */
    public static class ChatEventTitleChanged extends ChatEventAction {
        /**
         * Previous chat title.
         */
        public String oldTitle;
        /**
         * New chat title.
         */
        public String newTitle;

        /**
         * Default constructor.
         */
        public ChatEventTitleChanged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldTitle Previous chat title.
         * @param newTitle New chat title.
         */
        public ChatEventTitleChanged(String oldTitle, String newTitle) {
            this.oldTitle = oldTitle;
            this.newTitle = newTitle;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1134103250;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1134103250;
        }
    }

    /**
     * The chat description was changed.
     */
    public static class ChatEventDescriptionChanged extends ChatEventAction {
        /**
         * Previous chat description.
         */
        public String oldDescription;
        /**
         * New chat description.
         */
        public String newDescription;

        /**
         * Default constructor.
         */
        public ChatEventDescriptionChanged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldDescription Previous chat description.
         * @param newDescription New chat description.
         */
        public ChatEventDescriptionChanged(String oldDescription, String newDescription) {
            this.oldDescription = oldDescription;
            this.newDescription = newDescription;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 39112478;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 39112478;
        }
    }

    /**
     * The chat username was changed.
     */
    public static class ChatEventUsernameChanged extends ChatEventAction {
        /**
         * Previous chat username.
         */
        public String oldUsername;
        /**
         * New chat username.
         */
        public String newUsername;

        /**
         * Default constructor.
         */
        public ChatEventUsernameChanged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldUsername Previous chat username.
         * @param newUsername New chat username.
         */
        public ChatEventUsernameChanged(String oldUsername, String newUsername) {
            this.oldUsername = oldUsername;
            this.newUsername = newUsername;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1728558443;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1728558443;
        }
    }

    /**
     * The chat photo was changed.
     */
    public static class ChatEventPhotoChanged extends ChatEventAction {
        /**
         * Previous chat photo value; may be null.
         */
        public @Nullable ChatPhoto oldPhoto;
        /**
         * New chat photo value; may be null.
         */
        public @Nullable ChatPhoto newPhoto;

        /**
         * Default constructor.
         */
        public ChatEventPhotoChanged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldPhoto Previous chat photo value; may be null.
         * @param newPhoto New chat photo value; may be null.
         */
        public ChatEventPhotoChanged(ChatPhoto oldPhoto, ChatPhoto newPhoto) {
            this.oldPhoto = oldPhoto;
            this.newPhoto = newPhoto;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -811572541;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -811572541;
        }
    }

    /**
     * The anyoneCanInvite setting of a supergroup chat was toggled.
     */
    public static class ChatEventInvitesToggled extends ChatEventAction {
        /**
         * New value of anyoneCanInvite.
         */
        public boolean anyoneCanInvite;

        /**
         * Default constructor.
         */
        public ChatEventInvitesToggled() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param anyoneCanInvite New value of anyoneCanInvite.
         */
        public ChatEventInvitesToggled(boolean anyoneCanInvite) {
            this.anyoneCanInvite = anyoneCanInvite;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 568706937;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 568706937;
        }
    }

    /**
     * The signMessages setting of a channel was toggled.
     */
    public static class ChatEventSignMessagesToggled extends ChatEventAction {
        /**
         * New value of signMessages.
         */
        public boolean signMessages;

        /**
         * Default constructor.
         */
        public ChatEventSignMessagesToggled() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param signMessages New value of signMessages.
         */
        public ChatEventSignMessagesToggled(boolean signMessages) {
            this.signMessages = signMessages;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1313265634;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1313265634;
        }
    }

    /**
     * The supergroup sticker set was changed.
     */
    public static class ChatEventStickerSetChanged extends ChatEventAction {
        /**
         * Previous identifier of the chat sticker set; 0 if none.
         */
        public long oldStickerSetId;
        /**
         * New identifier of the chat sticker set; 0 if none.
         */
        public long newStickerSetId;

        /**
         * Default constructor.
         */
        public ChatEventStickerSetChanged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldStickerSetId Previous identifier of the chat sticker set; 0 if none.
         * @param newStickerSetId New identifier of the chat sticker set; 0 if none.
         */
        public ChatEventStickerSetChanged(long oldStickerSetId, long newStickerSetId) {
            this.oldStickerSetId = oldStickerSetId;
            this.newStickerSetId = newStickerSetId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1243130481;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1243130481;
        }
    }

    /**
     * The isAllHistoryAvailable setting of a supergroup was toggled.
     */
    public static class ChatEventIsAllHistoryAvailableToggled extends ChatEventAction {
        /**
         * New value of isAllHistoryAvailable.
         */
        public boolean isAllHistoryAvailable;

        /**
         * Default constructor.
         */
        public ChatEventIsAllHistoryAvailableToggled() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAllHistoryAvailable New value of isAllHistoryAvailable.
         */
        public ChatEventIsAllHistoryAvailableToggled(boolean isAllHistoryAvailable) {
            this.isAllHistoryAvailable = isAllHistoryAvailable;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1599063019;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1599063019;
        }
    }

    /**
     * Represents a set of filters used to obtain a chat event log.
     */
    public static class ChatEventLogFilters extends Object {
        /**
         * True, if message edits should be returned.
         */
        public boolean messageEdits;
        /**
         * True, if message deletions should be returned.
         */
        public boolean messageDeletions;
        /**
         * True, if pin/unpin events should be returned.
         */
        public boolean messagePins;
        /**
         * True, if members joining events should be returned.
         */
        public boolean memberJoins;
        /**
         * True, if members leaving events should be returned.
         */
        public boolean memberLeaves;
        /**
         * True, if invited member events should be returned.
         */
        public boolean memberInvites;
        /**
         * True, if member promotion/demotion events should be returned.
         */
        public boolean memberPromotions;
        /**
         * True, if member restricted/unrestricted/banned/unbanned events should be returned.
         */
        public boolean memberRestrictions;
        /**
         * True, if changes in chat information should be returned.
         */
        public boolean infoChanges;
        /**
         * True, if changes in chat settings should be returned.
         */
        public boolean settingChanges;

        /**
         * Default constructor.
         */
        public ChatEventLogFilters() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param messageEdits True, if message edits should be returned.
         * @param messageDeletions True, if message deletions should be returned.
         * @param messagePins True, if pin/unpin events should be returned.
         * @param memberJoins True, if members joining events should be returned.
         * @param memberLeaves True, if members leaving events should be returned.
         * @param memberInvites True, if invited member events should be returned.
         * @param memberPromotions True, if member promotion/demotion events should be returned.
         * @param memberRestrictions True, if member restricted/unrestricted/banned/unbanned events should be returned.
         * @param infoChanges True, if changes in chat information should be returned.
         * @param settingChanges True, if changes in chat settings should be returned.
         */
        public ChatEventLogFilters(boolean messageEdits, boolean messageDeletions, boolean messagePins, boolean memberJoins, boolean memberLeaves, boolean memberInvites, boolean memberPromotions, boolean memberRestrictions, boolean infoChanges, boolean settingChanges) {
            this.messageEdits = messageEdits;
            this.messageDeletions = messageDeletions;
            this.messagePins = messagePins;
            this.memberJoins = memberJoins;
            this.memberLeaves = memberLeaves;
            this.memberInvites = memberInvites;
            this.memberPromotions = memberPromotions;
            this.memberRestrictions = memberRestrictions;
            this.infoChanges = infoChanges;
            this.settingChanges = settingChanges;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 941939684;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 941939684;
        }
    }

    /**
     * Contains a list of chat events.
     */
    public static class ChatEvents extends Object {
        /**
         * List of events.
         */
        public ChatEvent[] events;

        /**
         * Default constructor.
         */
        public ChatEvents() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param events List of events.
         */
        public ChatEvents(ChatEvent[] events) {
            this.events = events;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -585329664;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -585329664;
        }
    }

    /**
     * Contains a chat invite link.
     */
    public static class ChatInviteLink extends Object {
        /**
         * Chat invite link.
         */
        public String inviteLink;

        /**
         * Default constructor.
         */
        public ChatInviteLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inviteLink Chat invite link.
         */
        public ChatInviteLink(String inviteLink) {
            this.inviteLink = inviteLink;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -882072492;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -882072492;
        }
    }

    /**
     * Contains information about a chat invite link.
     */
    public static class ChatInviteLinkInfo extends Object {
        /**
         * Chat identifier of the invite link; 0 if the user is not a member of this chat.
         */
        public long chatId;
        /**
         * Contains information about the type of the chat.
         */
        public ChatType type;
        /**
         * Title of the chat.
         */
        public String title;
        /**
         * Chat photo; may be null.
         */
        public @Nullable ChatPhoto photo;
        /**
         * Number of members.
         */
        public int memberCount;
        /**
         * User identifiers of some chat members that may be known to the current user.
         */
        public int[] memberUserIds;
        /**
         * True, if the chat is a public supergroup or channel with a username.
         */
        public boolean isPublic;

        /**
         * Default constructor.
         */
        public ChatInviteLinkInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the invite link; 0 if the user is not a member of this chat.
         * @param type Contains information about the type of the chat.
         * @param title Title of the chat.
         * @param photo Chat photo; may be null.
         * @param memberCount Number of members.
         * @param memberUserIds User identifiers of some chat members that may be known to the current user.
         * @param isPublic True, if the chat is a public supergroup or channel with a username.
         */
        public ChatInviteLinkInfo(long chatId, ChatType type, String title, ChatPhoto photo, int memberCount, int[] memberUserIds, boolean isPublic) {
            this.chatId = chatId;
            this.type = type;
            this.title = title;
            this.photo = photo;
            this.memberCount = memberCount;
            this.memberUserIds = memberUserIds;
            this.isPublic = isPublic;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -323394424;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -323394424;
        }
    }

    /**
     * A user with information about joining/leaving a chat.
     */
    public static class ChatMember extends Object {
        /**
         * User identifier of the chat member.
         */
        public int userId;
        /**
         * Identifier of a user that invited/promoted/banned this member in the chat; 0 if unknown.
         */
        public int inviterUserId;
        /**
         * Point in time (Unix timestamp) when the user joined a chat.
         */
        public int joinedChatDate;
        /**
         * Status of the member in the chat.
         */
        public ChatMemberStatus status;
        /**
         * If the user is a bot, information about the bot; may be null. Can be null even for a bot if the bot is not a chat member.
         */
        public @Nullable BotInfo botInfo;

        /**
         * Default constructor.
         */
        public ChatMember() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier of the chat member.
         * @param inviterUserId Identifier of a user that invited/promoted/banned this member in the chat; 0 if unknown.
         * @param joinedChatDate Point in time (Unix timestamp) when the user joined a chat.
         * @param status Status of the member in the chat.
         * @param botInfo If the user is a bot, information about the bot; may be null. Can be null even for a bot if the bot is not a chat member.
         */
        public ChatMember(int userId, int inviterUserId, int joinedChatDate, ChatMemberStatus status, BotInfo botInfo) {
            this.userId = userId;
            this.inviterUserId = inviterUserId;
            this.joinedChatDate = joinedChatDate;
            this.status = status;
            this.botInfo = botInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -806137076;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -806137076;
        }
    }

    /**
     * This class is an abstract base class.
     * Provides information about the status of a member in a chat.
     */
    public abstract static class ChatMemberStatus extends Object {
    }

    /**
     * The user is the creator of a chat and has all the administrator privileges.
     */
    public static class ChatMemberStatusCreator extends ChatMemberStatus {
        /**
         * True, if the user is a member of the chat.
         */
        public boolean isMember;

        /**
         * Default constructor.
         */
        public ChatMemberStatusCreator() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMember True, if the user is a member of the chat.
         */
        public ChatMemberStatusCreator(boolean isMember) {
            this.isMember = isMember;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1756320508;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1756320508;
        }
    }

    /**
     * The user is a member of a chat and has some additional privileges. In basic groups, administrators can edit and delete messages sent by others, add new members, and ban unprivileged members. In supergroups and channels, there are more detailed options for administrator privileges.
     */
    public static class ChatMemberStatusAdministrator extends ChatMemberStatus {
        /**
         * True, if the current user can edit the administrator privileges for the called user.
         */
        public boolean canBeEdited;
        /**
         * True, if the administrator can change the chat title, photo, and other settings.
         */
        public boolean canChangeInfo;
        /**
         * True, if the administrator can create channel posts; applicable to channels only.
         */
        public boolean canPostMessages;
        /**
         * True, if the administrator can edit messages of other users and pin messages; applicable to channels only.
         */
        public boolean canEditMessages;
        /**
         * True, if the administrator can delete messages of other users.
         */
        public boolean canDeleteMessages;
        /**
         * True, if the administrator can invite new users to the chat.
         */
        public boolean canInviteUsers;
        /**
         * True, if the administrator can restrict, ban, or unban chat members.
         */
        public boolean canRestrictMembers;
        /**
         * True, if the administrator can pin messages; applicable to supergroups only.
         */
        public boolean canPinMessages;
        /**
         * True, if the administrator can add new administrators with a subset of his own privileges or demote administrators that were directly or indirectly promoted by him.
         */
        public boolean canPromoteMembers;

        /**
         * Default constructor.
         */
        public ChatMemberStatusAdministrator() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param canBeEdited True, if the current user can edit the administrator privileges for the called user.
         * @param canChangeInfo True, if the administrator can change the chat title, photo, and other settings.
         * @param canPostMessages True, if the administrator can create channel posts; applicable to channels only.
         * @param canEditMessages True, if the administrator can edit messages of other users and pin messages; applicable to channels only.
         * @param canDeleteMessages True, if the administrator can delete messages of other users.
         * @param canInviteUsers True, if the administrator can invite new users to the chat.
         * @param canRestrictMembers True, if the administrator can restrict, ban, or unban chat members.
         * @param canPinMessages True, if the administrator can pin messages; applicable to supergroups only.
         * @param canPromoteMembers True, if the administrator can add new administrators with a subset of his own privileges or demote administrators that were directly or indirectly promoted by him.
         */
        public ChatMemberStatusAdministrator(boolean canBeEdited, boolean canChangeInfo, boolean canPostMessages, boolean canEditMessages, boolean canDeleteMessages, boolean canInviteUsers, boolean canRestrictMembers, boolean canPinMessages, boolean canPromoteMembers) {
            this.canBeEdited = canBeEdited;
            this.canChangeInfo = canChangeInfo;
            this.canPostMessages = canPostMessages;
            this.canEditMessages = canEditMessages;
            this.canDeleteMessages = canDeleteMessages;
            this.canInviteUsers = canInviteUsers;
            this.canRestrictMembers = canRestrictMembers;
            this.canPinMessages = canPinMessages;
            this.canPromoteMembers = canPromoteMembers;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 45106688;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 45106688;
        }
    }

    /**
     * The user is a member of a chat, without any additional privileges or restrictions.
     */
    public static class ChatMemberStatusMember extends ChatMemberStatus {

        /**
         * Default constructor.
         */
        public ChatMemberStatusMember() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 844723285;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 844723285;
        }
    }

    /**
     * The user is under certain restrictions in the chat. Not supported in basic groups and channels.
     */
    public static class ChatMemberStatusRestricted extends ChatMemberStatus {
        /**
         * True, if the user is a member of the chat.
         */
        public boolean isMember;
        /**
         * Point in time (Unix timestamp) when restrictions will be lifted from the user; 0 if never. If the user is restricted for more than 366 days or for less than 30 seconds from the current time, the user is considered to be restricted forever.
         */
        public int restrictedUntilDate;
        /**
         * True, if the user can send text messages, contacts, locations, and venues.
         */
        public boolean canSendMessages;
        /**
         * True, if the user can send audio files, documents, photos, videos, video notes, and voice notes. Implies canSendMessages permissions.
         */
        public boolean canSendMediaMessages;
        /**
         * True, if the user can send animations, games, and stickers and use inline bots. Implies canSendMediaMessages permissions.
         */
        public boolean canSendOtherMessages;
        /**
         * True, if the user may add a web page preview to his messages. Implies canSendMessages permissions.
         */
        public boolean canAddWebPagePreviews;

        /**
         * Default constructor.
         */
        public ChatMemberStatusRestricted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMember True, if the user is a member of the chat.
         * @param restrictedUntilDate Point in time (Unix timestamp) when restrictions will be lifted from the user; 0 if never. If the user is restricted for more than 366 days or for less than 30 seconds from the current time, the user is considered to be restricted forever.
         * @param canSendMessages True, if the user can send text messages, contacts, locations, and venues.
         * @param canSendMediaMessages True, if the user can send audio files, documents, photos, videos, video notes, and voice notes. Implies canSendMessages permissions.
         * @param canSendOtherMessages True, if the user can send animations, games, and stickers and use inline bots. Implies canSendMediaMessages permissions.
         * @param canAddWebPagePreviews True, if the user may add a web page preview to his messages. Implies canSendMessages permissions.
         */
        public ChatMemberStatusRestricted(boolean isMember, int restrictedUntilDate, boolean canSendMessages, boolean canSendMediaMessages, boolean canSendOtherMessages, boolean canAddWebPagePreviews) {
            this.isMember = isMember;
            this.restrictedUntilDate = restrictedUntilDate;
            this.canSendMessages = canSendMessages;
            this.canSendMediaMessages = canSendMediaMessages;
            this.canSendOtherMessages = canSendOtherMessages;
            this.canAddWebPagePreviews = canAddWebPagePreviews;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2068116214;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2068116214;
        }
    }

    /**
     * The user is not a chat member.
     */
    public static class ChatMemberStatusLeft extends ChatMemberStatus {

        /**
         * Default constructor.
         */
        public ChatMemberStatusLeft() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -5815259;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -5815259;
        }
    }

    /**
     * The user was banned (and hence is not a member of the chat). Implies the user can't return to the chat or view messages.
     */
    public static class ChatMemberStatusBanned extends ChatMemberStatus {
        /**
         * Point in time (Unix timestamp) when the user will be unbanned; 0 if never. If the user is banned for more than 366 days or for less than 30 seconds from the current time, the user is considered to be banned forever.
         */
        public int bannedUntilDate;

        /**
         * Default constructor.
         */
        public ChatMemberStatusBanned() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param bannedUntilDate Point in time (Unix timestamp) when the user will be unbanned; 0 if never. If the user is banned for more than 366 days or for less than 30 seconds from the current time, the user is considered to be banned forever.
         */
        public ChatMemberStatusBanned(int bannedUntilDate) {
            this.bannedUntilDate = bannedUntilDate;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1653518666;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1653518666;
        }
    }

    /**
     * Contains a list of chat members.
     */
    public static class ChatMembers extends Object {
        /**
         * Approximate total count of chat members found.
         */
        public int totalCount;
        /**
         * A list of chat members.
         */
        public ChatMember[] members;

        /**
         * Default constructor.
         */
        public ChatMembers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param totalCount Approximate total count of chat members found.
         * @param members A list of chat members.
         */
        public ChatMembers(int totalCount, ChatMember[] members) {
            this.totalCount = totalCount;
            this.members = members;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -497558622;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -497558622;
        }
    }

    /**
     * Describes the photo of a chat.
     */
    public static class ChatPhoto extends Object {
        /**
         * A small (160x160) chat photo.
         */
        public File small;
        /**
         * A big (640x640) chat photo.
         */
        public File big;

        /**
         * Default constructor.
         */
        public ChatPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param small A small (160x160) chat photo.
         * @param big A big (640x640) chat photo.
         */
        public ChatPhoto(File small, File big) {
            this.small = small;
            this.big = big;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -217062456;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -217062456;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the reason why a chat is reported.
     */
    public abstract static class ChatReportReason extends Object {
    }

    /**
     * The chat contains spam messages.
     */
    public static class ChatReportReasonSpam extends ChatReportReason {

        /**
         * Default constructor.
         */
        public ChatReportReasonSpam() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -510848863;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -510848863;
        }
    }

    /**
     * The chat promotes violence.
     */
    public static class ChatReportReasonViolence extends ChatReportReason {

        /**
         * Default constructor.
         */
        public ChatReportReasonViolence() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1330235395;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1330235395;
        }
    }

    /**
     * The chat contains pornographic messages.
     */
    public static class ChatReportReasonPornography extends ChatReportReason {

        /**
         * Default constructor.
         */
        public ChatReportReasonPornography() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 722614385;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 722614385;
        }
    }

    /**
     * A custom reason provided by the user.
     */
    public static class ChatReportReasonCustom extends ChatReportReason {
        /**
         * Report text.
         */
        public String text;

        /**
         * Default constructor.
         */
        public ChatReportReasonCustom() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Report text.
         */
        public ChatReportReasonCustom(String text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 544575454;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 544575454;
        }
    }

    /**
     * Contains information about the availability of the &quot;Report spam&quot; action for a chat.
     */
    public static class ChatReportSpamState extends Object {
        /**
         * True, if a prompt with the &quot;Report spam&quot; action should be shown to the user.
         */
        public boolean canReportSpam;

        /**
         * Default constructor.
         */
        public ChatReportSpamState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param canReportSpam True, if a prompt with the &quot;Report spam&quot; action should be shown to the user.
         */
        public ChatReportSpamState(boolean canReportSpam) {
            this.canReportSpam = canReportSpam;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1919240972;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1919240972;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the type of a chat.
     */
    public abstract static class ChatType extends Object {
    }

    /**
     * An ordinary chat with a user.
     */
    public static class ChatTypePrivate extends ChatType {
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public ChatTypePrivate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         */
        public ChatTypePrivate(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1700720838;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1700720838;
        }
    }

    /**
     * A basic group (i.e., a chat with 0-200 other users).
     */
    public static class ChatTypeBasicGroup extends ChatType {
        /**
         * Basic group identifier.
         */
        public int basicGroupId;

        /**
         * Default constructor.
         */
        public ChatTypeBasicGroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Basic group identifier.
         */
        public ChatTypeBasicGroup(int basicGroupId) {
            this.basicGroupId = basicGroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 21815278;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 21815278;
        }
    }

    /**
     * A supergroup (i.e. a chat with up to GetOption(&quot;supergroupMaxSize&quot;) other users), or channel (with unlimited members).
     */
    public static class ChatTypeSupergroup extends ChatType {
        /**
         * Supergroup or channel identifier.
         */
        public int supergroupId;
        /**
         * True, if the supergroup is a channel.
         */
        public boolean isChannel;

        /**
         * Default constructor.
         */
        public ChatTypeSupergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Supergroup or channel identifier.
         * @param isChannel True, if the supergroup is a channel.
         */
        public ChatTypeSupergroup(int supergroupId, boolean isChannel) {
            this.supergroupId = supergroupId;
            this.isChannel = isChannel;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 955152366;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 955152366;
        }
    }

    /**
     * A secret chat with a user.
     */
    public static class ChatTypeSecret extends ChatType {
        /**
         * Secret chat identifier.
         */
        public int secretChatId;
        /**
         * User identifier of the secret chat peer.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public ChatTypeSecret() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param secretChatId Secret chat identifier.
         * @param userId User identifier of the secret chat peer.
         */
        public ChatTypeSecret(int secretChatId, int userId) {
            this.secretChatId = secretChatId;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 136722563;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 136722563;
        }
    }

    /**
     * Represents a list of chats.
     */
    public static class Chats extends Object {
        /**
         * List of chat identifiers.
         */
        public long[] chatIds;

        /**
         * Default constructor.
         */
        public Chats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatIds List of chat identifiers.
         */
        public Chats(long[] chatIds) {
            this.chatIds = chatIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1687756019;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1687756019;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents result of checking whether a username can be set for a chat.
     */
    public abstract static class CheckChatUsernameResult extends Object {
    }

    /**
     * The username can be set.
     */
    public static class CheckChatUsernameResultOk extends CheckChatUsernameResult {

        /**
         * Default constructor.
         */
        public CheckChatUsernameResultOk() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1498956964;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1498956964;
        }
    }

    /**
     * The username is invalid.
     */
    public static class CheckChatUsernameResultUsernameInvalid extends CheckChatUsernameResult {

        /**
         * Default constructor.
         */
        public CheckChatUsernameResultUsernameInvalid() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -636979370;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -636979370;
        }
    }

    /**
     * The username is occupied.
     */
    public static class CheckChatUsernameResultUsernameOccupied extends CheckChatUsernameResult {

        /**
         * Default constructor.
         */
        public CheckChatUsernameResultUsernameOccupied() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1320892201;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1320892201;
        }
    }

    /**
     * The user has too much public chats, one of them should be made private first.
     */
    public static class CheckChatUsernameResultPublicChatsTooMuch extends CheckChatUsernameResult {

        /**
         * Default constructor.
         */
        public CheckChatUsernameResultPublicChatsTooMuch() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 858247741;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 858247741;
        }
    }

    /**
     * The user can't be a member of a public supergroup.
     */
    public static class CheckChatUsernameResultPublicGroupsUnavailable extends CheckChatUsernameResult {

        /**
         * Default constructor.
         */
        public CheckChatUsernameResultPublicGroupsUnavailable() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -51833641;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -51833641;
        }
    }

    /**
     * Contains information about one website the current user is logged in with Telegram.
     */
    public static class ConnectedWebsite extends Object {
        /**
         * Website identifier.
         */
        public long id;
        /**
         * The domain name of the website.
         */
        public String domainName;
        /**
         * User identifier of a bot linked with the website.
         */
        public int botUserId;
        /**
         * The version of a browser used to log in.
         */
        public String browser;
        /**
         * Operating system the browser is running on.
         */
        public String platform;
        /**
         * Point in time (Unix timestamp) when the user was logged in.
         */
        public int logInDate;
        /**
         * Point in time (Unix timestamp) when obtained authorization was last used.
         */
        public int lastActiveDate;
        /**
         * IP address from which the user was logged in, in human-readable format.
         */
        public String ip;
        /**
         * Human-readable description of a country and a region, from which the user was logged in, based on the IP address.
         */
        public String location;

        /**
         * Default constructor.
         */
        public ConnectedWebsite() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Website identifier.
         * @param domainName The domain name of the website.
         * @param botUserId User identifier of a bot linked with the website.
         * @param browser The version of a browser used to log in.
         * @param platform Operating system the browser is running on.
         * @param logInDate Point in time (Unix timestamp) when the user was logged in.
         * @param lastActiveDate Point in time (Unix timestamp) when obtained authorization was last used.
         * @param ip IP address from which the user was logged in, in human-readable format.
         * @param location Human-readable description of a country and a region, from which the user was logged in, based on the IP address.
         */
        public ConnectedWebsite(long id, String domainName, int botUserId, String browser, String platform, int logInDate, int lastActiveDate, String ip, String location) {
            this.id = id;
            this.domainName = domainName;
            this.botUserId = botUserId;
            this.browser = browser;
            this.platform = platform;
            this.logInDate = logInDate;
            this.lastActiveDate = lastActiveDate;
            this.ip = ip;
            this.location = location;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1538986855;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1538986855;
        }
    }

    /**
     * Contains a list of websites the current user is logged in with Telegram.
     */
    public static class ConnectedWebsites extends Object {
        /**
         * List of connected websites.
         */
        public ConnectedWebsite[] websites;

        /**
         * Default constructor.
         */
        public ConnectedWebsites() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param websites List of connected websites.
         */
        public ConnectedWebsites(ConnectedWebsite[] websites) {
            this.websites = websites;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1727949694;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1727949694;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the current state of the connection to Telegram servers.
     */
    public abstract static class ConnectionState extends Object {
    }

    /**
     * Currently waiting for the network to become available. Use SetNetworkType to change the available network type.
     */
    public static class ConnectionStateWaitingForNetwork extends ConnectionState {

        /**
         * Default constructor.
         */
        public ConnectionStateWaitingForNetwork() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1695405912;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1695405912;
        }
    }

    /**
     * Currently establishing a connection with a proxy server.
     */
    public static class ConnectionStateConnectingToProxy extends ConnectionState {

        /**
         * Default constructor.
         */
        public ConnectionStateConnectingToProxy() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -93187239;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -93187239;
        }
    }

    /**
     * Currently establishing a connection to the Telegram servers.
     */
    public static class ConnectionStateConnecting extends ConnectionState {

        /**
         * Default constructor.
         */
        public ConnectionStateConnecting() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1298400670;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1298400670;
        }
    }

    /**
     * Downloading data received while the client was offline.
     */
    public static class ConnectionStateUpdating extends ConnectionState {

        /**
         * Default constructor.
         */
        public ConnectionStateUpdating() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -188104009;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -188104009;
        }
    }

    /**
     * There is a working connection to the Telegram servers.
     */
    public static class ConnectionStateReady extends ConnectionState {

        /**
         * Default constructor.
         */
        public ConnectionStateReady() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 48608492;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 48608492;
        }
    }

    /**
     * Describes a user contact.
     */
    public static class Contact extends Object {
        /**
         * Phone number of the user.
         */
        public String phoneNumber;
        /**
         * First name of the user; 1-255 characters in length.
         */
        public String firstName;
        /**
         * Last name of the user.
         */
        public String lastName;
        /**
         * Identifier of the user, if known; otherwise 0.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public Contact() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param phoneNumber Phone number of the user.
         * @param firstName First name of the user; 1-255 characters in length.
         * @param lastName Last name of the user.
         * @param userId Identifier of the user, if known; otherwise 0.
         */
        public Contact(String phoneNumber, String firstName, String lastName, int userId) {
            this.phoneNumber = phoneNumber;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2035981269;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2035981269;
        }
    }

    /**
     * Contains a counter.
     */
    public static class Count extends Object {
        /**
         * Count.
         */
        public int count;

        /**
         * Default constructor.
         */
        public Count() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param count Count.
         */
        public Count(int count) {
            this.count = count;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1295577348;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1295577348;
        }
    }

    /**
     * Contains the result of a custom request.
     */
    public static class CustomRequestResult extends Object {
        /**
         * A JSON-serialized result.
         */
        public String result;

        /**
         * Default constructor.
         */
        public CustomRequestResult() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param result A JSON-serialized result.
         */
        public CustomRequestResult(String result) {
            this.result = result;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2009960452;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2009960452;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a data needed to subscribe for push notifications. To use specific push notification service, you must specify the correct application platform and upload valid server authentication data at https://my.telegram.org.
     */
    public abstract static class DeviceToken extends Object {
    }

    /**
     * A token for Google Cloud Messaging.
     */
    public static class DeviceTokenGoogleCloudMessaging extends DeviceToken {
        /**
         * Device registration token, may be empty to de-register a device.
         */
        public String token;

        /**
         * Default constructor.
         */
        public DeviceTokenGoogleCloudMessaging() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param token Device registration token, may be empty to de-register a device.
         */
        public DeviceTokenGoogleCloudMessaging(String token) {
            this.token = token;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1092220225;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1092220225;
        }
    }

    /**
     * A token for Apple Push Notification service.
     */
    public static class DeviceTokenApplePush extends DeviceToken {
        /**
         * Device token, may be empty to de-register a device.
         */
        public String deviceToken;
        /**
         * True, if App Sandbox is enabled.
         */
        public boolean isAppSandbox;

        /**
         * Default constructor.
         */
        public DeviceTokenApplePush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param deviceToken Device token, may be empty to de-register a device.
         * @param isAppSandbox True, if App Sandbox is enabled.
         */
        public DeviceTokenApplePush(String deviceToken, boolean isAppSandbox) {
            this.deviceToken = deviceToken;
            this.isAppSandbox = isAppSandbox;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 387541955;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 387541955;
        }
    }

    /**
     * A token for Apple Push Notification service VoIP notifications.
     */
    public static class DeviceTokenApplePushVoIP extends DeviceToken {
        /**
         * Device token, may be empty to de-register a device.
         */
        public String deviceToken;
        /**
         * True, if App Sandbox is enabled.
         */
        public boolean isAppSandbox;

        /**
         * Default constructor.
         */
        public DeviceTokenApplePushVoIP() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param deviceToken Device token, may be empty to de-register a device.
         * @param isAppSandbox True, if App Sandbox is enabled.
         */
        public DeviceTokenApplePushVoIP(String deviceToken, boolean isAppSandbox) {
            this.deviceToken = deviceToken;
            this.isAppSandbox = isAppSandbox;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -327676505;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -327676505;
        }
    }

    /**
     * A token for Windows Push Notification Services.
     */
    public static class DeviceTokenWindowsPush extends DeviceToken {
        /**
         * The access token that will be used to send notifications, may be empty to de-register a device.
         */
        public String accessToken;

        /**
         * Default constructor.
         */
        public DeviceTokenWindowsPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param accessToken The access token that will be used to send notifications, may be empty to de-register a device.
         */
        public DeviceTokenWindowsPush(String accessToken) {
            this.accessToken = accessToken;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1410514289;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1410514289;
        }
    }

    /**
     * A token for Microsoft Push Notification Service.
     */
    public static class DeviceTokenMicrosoftPush extends DeviceToken {
        /**
         * Push notification channel URI, may be empty to de-register a device.
         */
        public String channelUri;

        /**
         * Default constructor.
         */
        public DeviceTokenMicrosoftPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param channelUri Push notification channel URI, may be empty to de-register a device.
         */
        public DeviceTokenMicrosoftPush(String channelUri) {
            this.channelUri = channelUri;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1224269900;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1224269900;
        }
    }

    /**
     * A token for Microsoft Push Notification Service VoIP channel.
     */
    public static class DeviceTokenMicrosoftPushVoIP extends DeviceToken {
        /**
         * Push notification channel URI, may be empty to de-register a device.
         */
        public String channelUri;

        /**
         * Default constructor.
         */
        public DeviceTokenMicrosoftPushVoIP() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param channelUri Push notification channel URI, may be empty to de-register a device.
         */
        public DeviceTokenMicrosoftPushVoIP(String channelUri) {
            this.channelUri = channelUri;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -785603759;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -785603759;
        }
    }

    /**
     * A token for web Push API.
     */
    public static class DeviceTokenWebPush extends DeviceToken {
        /**
         * Absolute URL exposed by the push service where the application server can send push messages, may be empty to de-register a device.
         */
        public String endpoint;
        /**
         * Base64url-encoded P-256 elliptic curve Diffie-Hellman public key.
         */
        public String p256dhBase64url;
        /**
         * Base64url-encoded authentication secret.
         */
        public String authBase64url;

        /**
         * Default constructor.
         */
        public DeviceTokenWebPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param endpoint Absolute URL exposed by the push service where the application server can send push messages, may be empty to de-register a device.
         * @param p256dhBase64url Base64url-encoded P-256 elliptic curve Diffie-Hellman public key.
         * @param authBase64url Base64url-encoded authentication secret.
         */
        public DeviceTokenWebPush(String endpoint, String p256dhBase64url, String authBase64url) {
            this.endpoint = endpoint;
            this.p256dhBase64url = p256dhBase64url;
            this.authBase64url = authBase64url;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1694507273;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1694507273;
        }
    }

    /**
     * A token for Simple Push API for Firefox OS.
     */
    public static class DeviceTokenSimplePush extends DeviceToken {
        /**
         * Absolute URL exposed by the push service where the application server can send push messages, may be empty to de-register a device.
         */
        public String endpoint;

        /**
         * Default constructor.
         */
        public DeviceTokenSimplePush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param endpoint Absolute URL exposed by the push service where the application server can send push messages, may be empty to de-register a device.
         */
        public DeviceTokenSimplePush(String endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 49584736;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 49584736;
        }
    }

    /**
     * A token for Ubuntu Push Client service.
     */
    public static class DeviceTokenUbuntuPush extends DeviceToken {
        /**
         * Token, may be empty to de-register a device.
         */
        public String token;

        /**
         * Default constructor.
         */
        public DeviceTokenUbuntuPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param token Token, may be empty to de-register a device.
         */
        public DeviceTokenUbuntuPush(String token) {
            this.token = token;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1782320422;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1782320422;
        }
    }

    /**
     * A token for BlackBerry Push Service.
     */
    public static class DeviceTokenBlackBerryPush extends DeviceToken {
        /**
         * Token, may be empty to de-register a device.
         */
        public String token;

        /**
         * Default constructor.
         */
        public DeviceTokenBlackBerryPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param token Token, may be empty to de-register a device.
         */
        public DeviceTokenBlackBerryPush(String token) {
            this.token = token;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1559167234;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1559167234;
        }
    }

    /**
     * A token for Tizen Push Service.
     */
    public static class DeviceTokenTizenPush extends DeviceToken {
        /**
         * Push service registration identifier, may be empty to de-register a device.
         */
        public String regId;

        /**
         * Default constructor.
         */
        public DeviceTokenTizenPush() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param regId Push service registration identifier, may be empty to de-register a device.
         */
        public DeviceTokenTizenPush(String regId) {
            this.regId = regId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1359947213;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1359947213;
        }
    }

    /**
     * Describes a document of any type.
     */
    public static class Document extends Object {
        /**
         * Original name of the file; as defined by the sender.
         */
        public String fileName;
        /**
         * MIME type of the file; as defined by the sender.
         */
        public String mimeType;
        /**
         * Document thumbnail; as defined by the sender; may be null.
         */
        public @Nullable PhotoSize thumbnail;
        /**
         * File containing the document.
         */
        public File document;

        /**
         * Default constructor.
         */
        public Document() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileName Original name of the file; as defined by the sender.
         * @param mimeType MIME type of the file; as defined by the sender.
         * @param thumbnail Document thumbnail; as defined by the sender; may be null.
         * @param document File containing the document.
         */
        public Document(String fileName, String mimeType, PhotoSize thumbnail, File document) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.thumbnail = thumbnail;
            this.document = document;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -736037786;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -736037786;
        }
    }

    /**
     * Contains information about a message draft.
     */
    public static class DraftMessage extends Object {
        /**
         * Identifier of the message to reply to; 0 if none.
         */
        public long replyToMessageId;
        /**
         * Content of the message draft; this should always be of type inputMessageText.
         */
        public InputMessageContent inputMessageText;

        /**
         * Default constructor.
         */
        public DraftMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param replyToMessageId Identifier of the message to reply to; 0 if none.
         * @param inputMessageText Content of the message draft; this should always be of type inputMessageText.
         */
        public DraftMessage(long replyToMessageId, InputMessageContent inputMessageText) {
            this.replyToMessageId = replyToMessageId;
            this.inputMessageText = inputMessageText;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1902914742;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1902914742;
        }
    }

    /**
     * An object of this type can be returned on every function call, in case of an error.
     */
    public static class Error extends Object {
        /**
         * Error code; subject to future changes. If the error code is 406, the error message must not be processed in any way and must not be displayed to the user.
         */
        public int code;
        /**
         * Error message; subject to future changes.
         */
        public String message;

        /**
         * Default constructor.
         */
        public Error() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param code Error code; subject to future changes. If the error code is 406, the error message must not be processed in any way and must not be displayed to the user.
         * @param message Error message; subject to future changes.
         */
        public Error(int code, String message) {
            this.code = code;
            this.message = message;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1679978726;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1679978726;
        }
    }

    /**
     * Represents a file.
     */
    public static class File extends Object {
        /**
         * Unique file identifier.
         */
        public int id;
        /**
         * File size; 0 if unknown.
         */
        public int size;
        /**
         * Expected file size in case the exact file size is unknown, but an approximate size is known. Can be used to show download/upload progress.
         */
        public int expectedSize;
        /**
         * Information about the local copy of the file.
         */
        public LocalFile local;
        /**
         * Information about the remote copy of the file.
         */
        public RemoteFile remote;

        /**
         * Default constructor.
         */
        public File() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique file identifier.
         * @param size File size; 0 if unknown.
         * @param expectedSize Expected file size in case the exact file size is unknown, but an approximate size is known. Can be used to show download/upload progress.
         * @param local Information about the local copy of the file.
         * @param remote Information about the remote copy of the file.
         */
        public File(int id, int size, int expectedSize, LocalFile local, RemoteFile remote) {
            this.id = id;
            this.size = size;
            this.expectedSize = expectedSize;
            this.local = local;
            this.remote = remote;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 766337656;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 766337656;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the type of a file.
     */
    public abstract static class FileType extends Object {
    }

    /**
     * The data is not a file.
     */
    public static class FileTypeNone extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeNone() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2003009189;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2003009189;
        }
    }

    /**
     * The file is an animation.
     */
    public static class FileTypeAnimation extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeAnimation() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -290816582;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -290816582;
        }
    }

    /**
     * The file is an audio file.
     */
    public static class FileTypeAudio extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeAudio() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -709112160;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -709112160;
        }
    }

    /**
     * The file is a document.
     */
    public static class FileTypeDocument extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeDocument() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -564722929;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -564722929;
        }
    }

    /**
     * The file is a photo.
     */
    public static class FileTypePhoto extends FileType {

        /**
         * Default constructor.
         */
        public FileTypePhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1718914651;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1718914651;
        }
    }

    /**
     * The file is a profile photo.
     */
    public static class FileTypeProfilePhoto extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeProfilePhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1795089315;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1795089315;
        }
    }

    /**
     * The file was sent to a secret chat (the file type is not known to the server).
     */
    public static class FileTypeSecret extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeSecret() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1871899401;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1871899401;
        }
    }

    /**
     * The file is a sticker.
     */
    public static class FileTypeSticker extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeSticker() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 475233385;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 475233385;
        }
    }

    /**
     * The file is a thumbnail of another file.
     */
    public static class FileTypeThumbnail extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeThumbnail() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -12443298;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -12443298;
        }
    }

    /**
     * The file type is not yet known.
     */
    public static class FileTypeUnknown extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeUnknown() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2011566768;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2011566768;
        }
    }

    /**
     * The file is a video.
     */
    public static class FileTypeVideo extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeVideo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1430816539;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1430816539;
        }
    }

    /**
     * The file is a video note.
     */
    public static class FileTypeVideoNote extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeVideoNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -518412385;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -518412385;
        }
    }

    /**
     * The file is a voice note.
     */
    public static class FileTypeVoiceNote extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeVoiceNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -588681661;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -588681661;
        }
    }

    /**
     * The file is a wallpaper.
     */
    public static class FileTypeWallpaper extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeWallpaper() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1854930076;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1854930076;
        }
    }

    /**
     * The file is a thumbnail of a file from a secret chat.
     */
    public static class FileTypeSecretThumbnail extends FileType {

        /**
         * Default constructor.
         */
        public FileTypeSecretThumbnail() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1401326026;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1401326026;
        }
    }

    /**
     * A text with some entities.
     */
    public static class FormattedText extends Object {
        /**
         * The text.
         */
        public String text;
        /**
         * Entities contained in the text.
         */
        public TextEntity[] entities;

        /**
         * Default constructor.
         */
        public FormattedText() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text The text.
         * @param entities Entities contained in the text.
         */
        public FormattedText(String text, TextEntity[] entities) {
            this.text = text;
            this.entities = entities;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -252624564;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -252624564;
        }
    }

    /**
     * Contains a list of messages found by a search.
     */
    public static class FoundMessages extends Object {
        /**
         * List of messages.
         */
        public Message[] messages;
        /**
         * Value to pass as fromSearchId to get more results.
         */
        public long nextFromSearchId;

        /**
         * Default constructor.
         */
        public FoundMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param messages List of messages.
         * @param nextFromSearchId Value to pass as fromSearchId to get more results.
         */
        public FoundMessages(Message[] messages, long nextFromSearchId) {
            this.messages = messages;
            this.nextFromSearchId = nextFromSearchId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2135623881;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2135623881;
        }
    }

    /**
     * Describes a game.
     */
    public static class Game extends Object {
        /**
         * Game ID.
         */
        public long id;
        /**
         * Game short name. To share a game use the URL https://t.me/{botUsername}?game={gameShortName}.
         */
        public String shortName;
        /**
         * Game title.
         */
        public String title;
        /**
         * Game text, usually containing scoreboards for a game.
         */
        public FormattedText text;
        /**
         * Game description.
         */
        public String description;
        /**
         * Game photo.
         */
        public Photo photo;
        /**
         * Game animation; may be null.
         */
        public @Nullable Animation animation;

        /**
         * Default constructor.
         */
        public Game() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Game ID.
         * @param shortName Game short name. To share a game use the URL https://t.me/{botUsername}?game={gameShortName}.
         * @param title Game title.
         * @param text Game text, usually containing scoreboards for a game.
         * @param description Game description.
         * @param photo Game photo.
         * @param animation Game animation; may be null.
         */
        public Game(long id, String shortName, String title, FormattedText text, String description, Photo photo, Animation animation) {
            this.id = id;
            this.shortName = shortName;
            this.title = title;
            this.text = text;
            this.description = description;
            this.photo = photo;
            this.animation = animation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1565597752;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1565597752;
        }
    }

    /**
     * Contains one row of the game high score table.
     */
    public static class GameHighScore extends Object {
        /**
         * Position in the high score table.
         */
        public int position;
        /**
         * User identifier.
         */
        public int userId;
        /**
         * User score.
         */
        public int score;

        /**
         * Default constructor.
         */
        public GameHighScore() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param position Position in the high score table.
         * @param userId User identifier.
         * @param score User score.
         */
        public GameHighScore(int position, int userId, int score) {
            this.position = position;
            this.userId = userId;
            this.score = score;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -30778358;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -30778358;
        }
    }

    /**
     * Contains a list of game high scores.
     */
    public static class GameHighScores extends Object {
        /**
         * A list of game high scores.
         */
        public GameHighScore[] scores;

        /**
         * Default constructor.
         */
        public GameHighScores() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param scores A list of game high scores.
         */
        public GameHighScores(GameHighScore[] scores) {
            this.scores = scores;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -725770727;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -725770727;
        }
    }

    /**
     * Contains a list of hashtags.
     */
    public static class Hashtags extends Object {
        /**
         * A list of hashtags.
         */
        public String[] hashtags;

        /**
         * Default constructor.
         */
        public Hashtags() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param hashtags A list of hashtags.
         */
        public Hashtags(String[] hashtags) {
            this.hashtags = hashtags;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 676798885;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 676798885;
        }
    }

    /**
     * Represents the result of an ImportContacts request.
     */
    public static class ImportedContacts extends Object {
        /**
         * User identifiers of the imported contacts in the same order as they were specified in the request; 0 if the contact is not yet a registered user.
         */
        public int[] userIds;
        /**
         * The number of users that imported the corresponding contact; 0 for already registered users or if unavailable.
         */
        public int[] importerCount;

        /**
         * Default constructor.
         */
        public ImportedContacts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userIds User identifiers of the imported contacts in the same order as they were specified in the request; 0 if the contact is not yet a registered user.
         * @param importerCount The number of users that imported the corresponding contact; 0 for already registered users or if unavailable.
         */
        public ImportedContacts(int[] userIds, int[] importerCount) {
            this.userIds = userIds;
            this.importerCount = importerCount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -741685354;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -741685354;
        }
    }

    /**
     * Represents a single button in an inline keyboard.
     */
    public static class InlineKeyboardButton extends Object {
        /**
         * Text of the button.
         */
        public String text;
        /**
         * Type of the button.
         */
        public InlineKeyboardButtonType type;

        /**
         * Default constructor.
         */
        public InlineKeyboardButton() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text of the button.
         * @param type Type of the button.
         */
        public InlineKeyboardButton(String text, InlineKeyboardButtonType type) {
            this.text = text;
            this.type = type;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -372105704;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -372105704;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the type of an inline keyboard button.
     */
    public abstract static class InlineKeyboardButtonType extends Object {
    }

    /**
     * A button that opens a specified URL.
     */
    public static class InlineKeyboardButtonTypeUrl extends InlineKeyboardButtonType {
        /**
         * URL to open.
         */
        public String url;

        /**
         * Default constructor.
         */
        public InlineKeyboardButtonTypeUrl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url URL to open.
         */
        public InlineKeyboardButtonTypeUrl(String url) {
            this.url = url;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1130741420;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1130741420;
        }
    }

    /**
     * A button that sends a special callback query to a bot.
     */
    public static class InlineKeyboardButtonTypeCallback extends InlineKeyboardButtonType {
        /**
         * Data to be sent to the bot via a callback query.
         */
        public byte[] data;

        /**
         * Default constructor.
         */
        public InlineKeyboardButtonTypeCallback() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param data Data to be sent to the bot via a callback query.
         */
        public InlineKeyboardButtonTypeCallback(byte[] data) {
            this.data = data;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1127515139;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1127515139;
        }
    }

    /**
     * A button with a game that sends a special callback query to a bot. This button must be in the first column and row of the keyboard and can be attached only to a message with content of the type messageGame.
     */
    public static class InlineKeyboardButtonTypeCallbackGame extends InlineKeyboardButtonType {

        /**
         * Default constructor.
         */
        public InlineKeyboardButtonTypeCallbackGame() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -383429528;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -383429528;
        }
    }

    /**
     * A button that forces an inline query to the bot to be inserted in the input field.
     */
    public static class InlineKeyboardButtonTypeSwitchInline extends InlineKeyboardButtonType {
        /**
         * Inline query to be sent to the bot.
         */
        public String query;
        /**
         * True, if the inline query should be sent from the current chat.
         */
        public boolean inCurrentChat;

        /**
         * Default constructor.
         */
        public InlineKeyboardButtonTypeSwitchInline() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Inline query to be sent to the bot.
         * @param inCurrentChat True, if the inline query should be sent from the current chat.
         */
        public InlineKeyboardButtonTypeSwitchInline(String query, boolean inCurrentChat) {
            this.query = query;
            this.inCurrentChat = inCurrentChat;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2035563307;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2035563307;
        }
    }

    /**
     * A button to buy something. This button must be in the first column and row of the keyboard and can be attached only to a message with content of the type messageInvoice.
     */
    public static class InlineKeyboardButtonTypeBuy extends InlineKeyboardButtonType {

        /**
         * Default constructor.
         */
        public InlineKeyboardButtonTypeBuy() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1360739440;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1360739440;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a single result of an inline query.
     */
    public abstract static class InlineQueryResult extends Object {
    }

    /**
     * Represents a link to an article or web page.
     */
    public static class InlineQueryResultArticle extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * URL of the result, if it exists.
         */
        public String url;
        /**
         * True, if the URL must be not shown.
         */
        public boolean hideUrl;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * A short description of the result.
         */
        public String description;
        /**
         * Result thumbnail; may be null.
         */
        public @Nullable PhotoSize thumbnail;

        /**
         * Default constructor.
         */
        public InlineQueryResultArticle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param url URL of the result, if it exists.
         * @param hideUrl True, if the URL must be not shown.
         * @param title Title of the result.
         * @param description A short description of the result.
         * @param thumbnail Result thumbnail; may be null.
         */
        public InlineQueryResultArticle(String id, String url, boolean hideUrl, String title, String description, PhotoSize thumbnail) {
            this.id = id;
            this.url = url;
            this.hideUrl = hideUrl;
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -518366710;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -518366710;
        }
    }

    /**
     * Represents a user contact.
     */
    public static class InlineQueryResultContact extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * A user contact.
         */
        public Contact contact;
        /**
         * Result thumbnail; may be null.
         */
        public @Nullable PhotoSize thumbnail;

        /**
         * Default constructor.
         */
        public InlineQueryResultContact() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param contact A user contact.
         * @param thumbnail Result thumbnail; may be null.
         */
        public InlineQueryResultContact(String id, Contact contact, PhotoSize thumbnail) {
            this.id = id;
            this.contact = contact;
            this.thumbnail = thumbnail;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 410081985;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 410081985;
        }
    }

    /**
     * Represents a point on the map.
     */
    public static class InlineQueryResultLocation extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Location result.
         */
        public Location location;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * Result thumbnail; may be null.
         */
        public @Nullable PhotoSize thumbnail;

        /**
         * Default constructor.
         */
        public InlineQueryResultLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param location Location result.
         * @param title Title of the result.
         * @param thumbnail Result thumbnail; may be null.
         */
        public InlineQueryResultLocation(String id, Location location, String title, PhotoSize thumbnail) {
            this.id = id;
            this.location = location;
            this.title = title;
            this.thumbnail = thumbnail;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -158305341;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -158305341;
        }
    }

    /**
     * Represents information about a venue.
     */
    public static class InlineQueryResultVenue extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Venue result.
         */
        public Venue venue;
        /**
         * Result thumbnail; may be null.
         */
        public @Nullable PhotoSize thumbnail;

        /**
         * Default constructor.
         */
        public InlineQueryResultVenue() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param venue Venue result.
         * @param thumbnail Result thumbnail; may be null.
         */
        public InlineQueryResultVenue(String id, Venue venue, PhotoSize thumbnail) {
            this.id = id;
            this.venue = venue;
            this.thumbnail = thumbnail;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1592932211;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1592932211;
        }
    }

    /**
     * Represents information about a game.
     */
    public static class InlineQueryResultGame extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Game result.
         */
        public Game game;

        /**
         * Default constructor.
         */
        public InlineQueryResultGame() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param game Game result.
         */
        public InlineQueryResultGame(String id, Game game) {
            this.id = id;
            this.game = game;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1706916987;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1706916987;
        }
    }

    /**
     * Represents an animation file.
     */
    public static class InlineQueryResultAnimation extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Animation file.
         */
        public Animation animation;
        /**
         * Animation title.
         */
        public String title;

        /**
         * Default constructor.
         */
        public InlineQueryResultAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param animation Animation file.
         * @param title Animation title.
         */
        public InlineQueryResultAnimation(String id, Animation animation, String title) {
            this.id = id;
            this.animation = animation;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2009984267;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2009984267;
        }
    }

    /**
     * Represents an audio file.
     */
    public static class InlineQueryResultAudio extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Audio file.
         */
        public Audio audio;

        /**
         * Default constructor.
         */
        public InlineQueryResultAudio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param audio Audio file.
         */
        public InlineQueryResultAudio(String id, Audio audio) {
            this.id = id;
            this.audio = audio;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 842650360;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 842650360;
        }
    }

    /**
     * Represents a document.
     */
    public static class InlineQueryResultDocument extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Document.
         */
        public Document document;
        /**
         * Document title.
         */
        public String title;
        /**
         * Document description.
         */
        public String description;

        /**
         * Default constructor.
         */
        public InlineQueryResultDocument() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param document Document.
         * @param title Document title.
         * @param description Document description.
         */
        public InlineQueryResultDocument(String id, Document document, String title, String description) {
            this.id = id;
            this.document = document;
            this.title = title;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1491268539;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1491268539;
        }
    }

    /**
     * Represents a photo.
     */
    public static class InlineQueryResultPhoto extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Photo.
         */
        public Photo photo;
        /**
         * Title of the result, if known.
         */
        public String title;
        /**
         * A short description of the result, if known.
         */
        public String description;

        /**
         * Default constructor.
         */
        public InlineQueryResultPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param photo Photo.
         * @param title Title of the result, if known.
         * @param description A short description of the result, if known.
         */
        public InlineQueryResultPhoto(String id, Photo photo, String title, String description) {
            this.id = id;
            this.photo = photo;
            this.title = title;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1848319440;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1848319440;
        }
    }

    /**
     * Represents a sticker.
     */
    public static class InlineQueryResultSticker extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Sticker.
         */
        public Sticker sticker;

        /**
         * Default constructor.
         */
        public InlineQueryResultSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param sticker Sticker.
         */
        public InlineQueryResultSticker(String id, Sticker sticker) {
            this.id = id;
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1848224245;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1848224245;
        }
    }

    /**
     * Represents a video.
     */
    public static class InlineQueryResultVideo extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Video.
         */
        public Video video;
        /**
         * Title of the video.
         */
        public String title;
        /**
         * Description of the video.
         */
        public String description;

        /**
         * Default constructor.
         */
        public InlineQueryResultVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param video Video.
         * @param title Title of the video.
         * @param description Description of the video.
         */
        public InlineQueryResultVideo(String id, Video video, String title, String description) {
            this.id = id;
            this.video = video;
            this.title = title;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1373158683;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1373158683;
        }
    }

    /**
     * Represents a voice note.
     */
    public static class InlineQueryResultVoiceNote extends InlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Voice note.
         */
        public VoiceNote voiceNote;
        /**
         * Title of the voice note.
         */
        public String title;

        /**
         * Default constructor.
         */
        public InlineQueryResultVoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param voiceNote Voice note.
         * @param title Title of the voice note.
         */
        public InlineQueryResultVoiceNote(String id, VoiceNote voiceNote, String title) {
            this.id = id;
            this.voiceNote = voiceNote;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1897393105;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1897393105;
        }
    }

    /**
     * Represents the results of the inline query. Use sendInlineQueryResultMessage to send the result of the query.
     */
    public static class InlineQueryResults extends Object {
        /**
         * Unique identifier of the inline query.
         */
        public long inlineQueryId;
        /**
         * The offset for the next request. If empty, there are no more results.
         */
        public String nextOffset;
        /**
         * Results of the query.
         */
        public InlineQueryResult[] results;
        /**
         * If non-empty, this text should be shown on the button, which opens a private chat with the bot and sends the bot a start message with the switchPmParameter.
         */
        public String switchPmText;
        /**
         * Parameter for the bot start message.
         */
        public String switchPmParameter;

        /**
         * Default constructor.
         */
        public InlineQueryResults() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineQueryId Unique identifier of the inline query.
         * @param nextOffset The offset for the next request. If empty, there are no more results.
         * @param results Results of the query.
         * @param switchPmText If non-empty, this text should be shown on the button, which opens a private chat with the bot and sends the bot a start message with the switchPmParameter.
         * @param switchPmParameter Parameter for the bot start message.
         */
        public InlineQueryResults(long inlineQueryId, String nextOffset, InlineQueryResult[] results, String switchPmText, String switchPmParameter) {
            this.inlineQueryId = inlineQueryId;
            this.nextOffset = nextOffset;
            this.results = results;
            this.switchPmText = switchPmText;
            this.switchPmParameter = switchPmParameter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1000709656;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1000709656;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains information about the payment method chosen by the user.
     */
    public abstract static class InputCredentials extends Object {
    }

    /**
     * Applies if a user chooses some previously saved payment credentials. To use their previously saved credentials, the user must have a valid temporary password.
     */
    public static class InputCredentialsSaved extends InputCredentials {
        /**
         * Identifier of the saved credentials.
         */
        public String savedCredentialsId;

        /**
         * Default constructor.
         */
        public InputCredentialsSaved() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param savedCredentialsId Identifier of the saved credentials.
         */
        public InputCredentialsSaved(String savedCredentialsId) {
            this.savedCredentialsId = savedCredentialsId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2034385364;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2034385364;
        }
    }

    /**
     * Applies if a user enters new credentials on a payment provider website.
     */
    public static class InputCredentialsNew extends InputCredentials {
        /**
         * Contains JSON-encoded data with a credential identifier from the payment provider.
         */
        public String data;
        /**
         * True, if the credential identifier can be saved on the server side.
         */
        public boolean allowSave;

        /**
         * Default constructor.
         */
        public InputCredentialsNew() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param data Contains JSON-encoded data with a credential identifier from the payment provider.
         * @param allowSave True, if the credential identifier can be saved on the server side.
         */
        public InputCredentialsNew(String data, boolean allowSave) {
            this.data = data;
            this.allowSave = allowSave;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -829689558;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -829689558;
        }
    }

    /**
     * Applies if a user enters new credentials using Android Pay.
     */
    public static class InputCredentialsAndroidPay extends InputCredentials {
        /**
         * JSON-encoded data with the credential identifier.
         */
        public String data;

        /**
         * Default constructor.
         */
        public InputCredentialsAndroidPay() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param data JSON-encoded data with the credential identifier.
         */
        public InputCredentialsAndroidPay(String data) {
            this.data = data;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1979566832;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1979566832;
        }
    }

    /**
     * Applies if a user enters new credentials using Apple Pay.
     */
    public static class InputCredentialsApplePay extends InputCredentials {
        /**
         * JSON-encoded data with the credential identifier.
         */
        public String data;

        /**
         * Default constructor.
         */
        public InputCredentialsApplePay() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param data JSON-encoded data with the credential identifier.
         */
        public InputCredentialsApplePay(String data) {
            this.data = data;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1246570799;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1246570799;
        }
    }

    /**
     * This class is an abstract base class.
     * Points to a file.
     */
    public abstract static class InputFile extends Object {
    }

    /**
     * A file defined by its unique ID.
     */
    public static class InputFileId extends InputFile {
        /**
         * Unique file identifier.
         */
        public int id;

        /**
         * Default constructor.
         */
        public InputFileId() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique file identifier.
         */
        public InputFileId(int id) {
            this.id = id;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1788906253;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1788906253;
        }
    }

    /**
     * A file defined by its remote ID.
     */
    public static class InputFileRemote extends InputFile {
        /**
         * Remote file identifier.
         */
        public String id;

        /**
         * Default constructor.
         */
        public InputFileRemote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Remote file identifier.
         */
        public InputFileRemote(String id) {
            this.id = id;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -107574466;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -107574466;
        }
    }

    /**
     * A file defined by a local path.
     */
    public static class InputFileLocal extends InputFile {
        /**
         * Local path to the file.
         */
        public String path;

        /**
         * Default constructor.
         */
        public InputFileLocal() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param path Local path to the file.
         */
        public InputFileLocal(String path) {
            this.path = path;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2056030919;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2056030919;
        }
    }

    /**
     * A file generated by the client.
     */
    public static class InputFileGenerated extends InputFile {
        /**
         * Local path to a file from which the file is generated, may be empty if there is no such file.
         */
        public String originalPath;
        /**
         * String specifying the conversion applied to the original file; should be persistent across application restarts.
         */
        public String conversion;
        /**
         * Expected size of the generated file; 0 if unknown.
         */
        public int expectedSize;

        /**
         * Default constructor.
         */
        public InputFileGenerated() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param originalPath Local path to a file from which the file is generated, may be empty if there is no such file.
         * @param conversion String specifying the conversion applied to the original file; should be persistent across application restarts.
         * @param expectedSize Expected size of the generated file; 0 if unknown.
         */
        public InputFileGenerated(String originalPath, String conversion, int expectedSize) {
            this.originalPath = originalPath;
            this.conversion = conversion;
            this.expectedSize = expectedSize;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1781351885;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1781351885;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a single result of an inline query; for bots only.
     */
    public abstract static class InputInlineQueryResult extends Object {
    }

    /**
     * Represents a link to an animated GIF.
     */
    public static class InputInlineQueryResultAnimatedGif extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the query result.
         */
        public String title;
        /**
         * URL of the static result thumbnail (JPEG or GIF), if it exists.
         */
        public String thumbnailUrl;
        /**
         * The URL of the GIF-file (file size must not exceed 1MB).
         */
        public String gifUrl;
        /**
         * Duration of the GIF, in seconds.
         */
        public int gifDuration;
        /**
         * Width of the GIF.
         */
        public int gifWidth;
        /**
         * Height of the GIF.
         */
        public int gifHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAnimation, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultAnimatedGif() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the query result.
         * @param thumbnailUrl URL of the static result thumbnail (JPEG or GIF), if it exists.
         * @param gifUrl The URL of the GIF-file (file size must not exceed 1MB).
         * @param gifDuration Duration of the GIF, in seconds.
         * @param gifWidth Width of the GIF.
         * @param gifHeight Height of the GIF.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAnimation, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultAnimatedGif(String id, String title, String thumbnailUrl, String gifUrl, int gifDuration, int gifWidth, int gifHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.gifUrl = gifUrl;
            this.gifDuration = gifDuration;
            this.gifWidth = gifWidth;
            this.gifHeight = gifHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -891474894;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -891474894;
        }
    }

    /**
     * Represents a link to an animated (i.e. without sound) H.264/MPEG-4 AVC video.
     */
    public static class InputInlineQueryResultAnimatedMpeg4 extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * URL of the static result thumbnail (JPEG or GIF), if it exists.
         */
        public String thumbnailUrl;
        /**
         * The URL of the MPEG4-file (file size must not exceed 1MB).
         */
        public String mpeg4Url;
        /**
         * Duration of the video, in seconds.
         */
        public int mpeg4Duration;
        /**
         * Width of the video.
         */
        public int mpeg4Width;
        /**
         * Height of the video.
         */
        public int mpeg4Height;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAnimation, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultAnimatedMpeg4() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the result.
         * @param thumbnailUrl URL of the static result thumbnail (JPEG or GIF), if it exists.
         * @param mpeg4Url The URL of the MPEG4-file (file size must not exceed 1MB).
         * @param mpeg4Duration Duration of the video, in seconds.
         * @param mpeg4Width Width of the video.
         * @param mpeg4Height Height of the video.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAnimation, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultAnimatedMpeg4(String id, String title, String thumbnailUrl, String mpeg4Url, int mpeg4Duration, int mpeg4Width, int mpeg4Height, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.mpeg4Url = mpeg4Url;
            this.mpeg4Duration = mpeg4Duration;
            this.mpeg4Width = mpeg4Width;
            this.mpeg4Height = mpeg4Height;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1629529888;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1629529888;
        }
    }

    /**
     * Represents a link to an article or web page.
     */
    public static class InputInlineQueryResultArticle extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * URL of the result, if it exists.
         */
        public String url;
        /**
         * True, if the URL must be not shown.
         */
        public boolean hideUrl;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * A short description of the result.
         */
        public String description;
        /**
         * URL of the result thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * Thumbnail width, if known.
         */
        public int thumbnailWidth;
        /**
         * Thumbnail height, if known.
         */
        public int thumbnailHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultArticle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param url URL of the result, if it exists.
         * @param hideUrl True, if the URL must be not shown.
         * @param title Title of the result.
         * @param description A short description of the result.
         * @param thumbnailUrl URL of the result thumbnail, if it exists.
         * @param thumbnailWidth Thumbnail width, if known.
         * @param thumbnailHeight Thumbnail height, if known.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultArticle(String id, String url, boolean hideUrl, String title, String description, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.url = url;
            this.hideUrl = hideUrl;
            this.title = title;
            this.description = description;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1973670156;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1973670156;
        }
    }

    /**
     * Represents a link to an MP3 audio file.
     */
    public static class InputInlineQueryResultAudio extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the audio file.
         */
        public String title;
        /**
         * Performer of the audio file.
         */
        public String performer;
        /**
         * The URL of the audio file.
         */
        public String audioUrl;
        /**
         * Audio file duration, in seconds.
         */
        public int audioDuration;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAudio, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultAudio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the audio file.
         * @param performer Performer of the audio file.
         * @param audioUrl The URL of the audio file.
         * @param audioDuration Audio file duration, in seconds.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageAudio, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultAudio(String id, String title, String performer, String audioUrl, int audioDuration, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.performer = performer;
            this.audioUrl = audioUrl;
            this.audioDuration = audioDuration;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1260139988;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1260139988;
        }
    }

    /**
     * Represents a user contact.
     */
    public static class InputInlineQueryResultContact extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * User contact.
         */
        public Contact contact;
        /**
         * URL of the result thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * Thumbnail width, if known.
         */
        public int thumbnailWidth;
        /**
         * Thumbnail height, if known.
         */
        public int thumbnailHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultContact() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param contact User contact.
         * @param thumbnailUrl URL of the result thumbnail, if it exists.
         * @param thumbnailWidth Thumbnail width, if known.
         * @param thumbnailHeight Thumbnail height, if known.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultContact(String id, Contact contact, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.contact = contact;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1846064594;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1846064594;
        }
    }

    /**
     * Represents a link to a file.
     */
    public static class InputInlineQueryResultDocument extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the resulting file.
         */
        public String title;
        /**
         * Short description of the result, if known.
         */
        public String description;
        /**
         * URL of the file.
         */
        public String documentUrl;
        /**
         * MIME type of the file content; only &quot;application/pdf&quot; and &quot;application/zip&quot; are currently allowed.
         */
        public String mimeType;
        /**
         * The URL of the file thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * Width of the thumbnail.
         */
        public int thumbnailWidth;
        /**
         * Height of the thumbnail.
         */
        public int thumbnailHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageDocument, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultDocument() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the resulting file.
         * @param description Short description of the result, if known.
         * @param documentUrl URL of the file.
         * @param mimeType MIME type of the file content; only &quot;application/pdf&quot; and &quot;application/zip&quot; are currently allowed.
         * @param thumbnailUrl The URL of the file thumbnail, if it exists.
         * @param thumbnailWidth Width of the thumbnail.
         * @param thumbnailHeight Height of the thumbnail.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageDocument, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultDocument(String id, String title, String description, String documentUrl, String mimeType, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.documentUrl = documentUrl;
            this.mimeType = mimeType;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 578801869;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 578801869;
        }
    }

    /**
     * Represents a game.
     */
    public static class InputInlineQueryResultGame extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Short name of the game.
         */
        public String gameShortName;
        /**
         * Message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultGame() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param gameShortName Short name of the game.
         * @param replyMarkup Message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public InputInlineQueryResultGame(String id, String gameShortName, ReplyMarkup replyMarkup) {
            this.id = id;
            this.gameShortName = gameShortName;
            this.replyMarkup = replyMarkup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 966074327;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 966074327;
        }
    }

    /**
     * Represents a point on the map.
     */
    public static class InputInlineQueryResultLocation extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Location result.
         */
        public Location location;
        /**
         * Amount of time relative to the message sent time until the location can be updated, in seconds.
         */
        public int livePeriod;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * URL of the result thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * Thumbnail width, if known.
         */
        public int thumbnailWidth;
        /**
         * Thumbnail height, if known.
         */
        public int thumbnailHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param location Location result.
         * @param livePeriod Amount of time relative to the message sent time until the location can be updated, in seconds.
         * @param title Title of the result.
         * @param thumbnailUrl URL of the result thumbnail, if it exists.
         * @param thumbnailWidth Thumbnail width, if known.
         * @param thumbnailHeight Thumbnail height, if known.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultLocation(String id, Location location, int livePeriod, String title, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.location = location;
            this.livePeriod = livePeriod;
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1887650218;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1887650218;
        }
    }

    /**
     * Represents link to a JPEG image.
     */
    public static class InputInlineQueryResultPhoto extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the result, if known.
         */
        public String title;
        /**
         * A short description of the result, if known.
         */
        public String description;
        /**
         * URL of the photo thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * The URL of the JPEG photo (photo size must not exceed 5MB).
         */
        public String photoUrl;
        /**
         * Width of the photo.
         */
        public int photoWidth;
        /**
         * Height of the photo.
         */
        public int photoHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessagePhoto, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the result, if known.
         * @param description A short description of the result, if known.
         * @param thumbnailUrl URL of the photo thumbnail, if it exists.
         * @param photoUrl The URL of the JPEG photo (photo size must not exceed 5MB).
         * @param photoWidth Width of the photo.
         * @param photoHeight Height of the photo.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessagePhoto, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultPhoto(String id, String title, String description, String thumbnailUrl, String photoUrl, int photoWidth, int photoHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.thumbnailUrl = thumbnailUrl;
            this.photoUrl = photoUrl;
            this.photoWidth = photoWidth;
            this.photoHeight = photoHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1123338721;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1123338721;
        }
    }

    /**
     * Represents a link to a WEBP sticker.
     */
    public static class InputInlineQueryResultSticker extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * URL of the sticker thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * The URL of the WEBP sticker (sticker file size must not exceed 5MB).
         */
        public String stickerUrl;
        /**
         * Width of the sticker.
         */
        public int stickerWidth;
        /**
         * Height of the sticker.
         */
        public int stickerHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, inputMessageSticker, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param thumbnailUrl URL of the sticker thumbnail, if it exists.
         * @param stickerUrl The URL of the WEBP sticker (sticker file size must not exceed 5MB).
         * @param stickerWidth Width of the sticker.
         * @param stickerHeight Height of the sticker.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, inputMessageSticker, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultSticker(String id, String thumbnailUrl, String stickerUrl, int stickerWidth, int stickerHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.thumbnailUrl = thumbnailUrl;
            this.stickerUrl = stickerUrl;
            this.stickerWidth = stickerWidth;
            this.stickerHeight = stickerHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 274007129;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 274007129;
        }
    }

    /**
     * Represents information about a venue.
     */
    public static class InputInlineQueryResultVenue extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Venue result.
         */
        public Venue venue;
        /**
         * URL of the result thumbnail, if it exists.
         */
        public String thumbnailUrl;
        /**
         * Thumbnail width, if known.
         */
        public int thumbnailWidth;
        /**
         * Thumbnail height, if known.
         */
        public int thumbnailHeight;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultVenue() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param venue Venue result.
         * @param thumbnailUrl URL of the result thumbnail, if it exists.
         * @param thumbnailWidth Thumbnail width, if known.
         * @param thumbnailHeight Thumbnail height, if known.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultVenue(String id, Venue venue, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.venue = venue;
            this.thumbnailUrl = thumbnailUrl;
            this.thumbnailWidth = thumbnailWidth;
            this.thumbnailHeight = thumbnailHeight;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 541704509;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 541704509;
        }
    }

    /**
     * Represents a link to a page containing an embedded video player or a video file.
     */
    public static class InputInlineQueryResultVideo extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the result.
         */
        public String title;
        /**
         * A short description of the result, if known.
         */
        public String description;
        /**
         * The URL of the video thumbnail (JPEG), if it exists.
         */
        public String thumbnailUrl;
        /**
         * URL of the embedded video player or video file.
         */
        public String videoUrl;
        /**
         * MIME type of the content of the video URL, only &quot;text/html&quot; or &quot;video/mp4&quot; are currently supported.
         */
        public String mimeType;
        /**
         * Width of the video.
         */
        public int videoWidth;
        /**
         * Height of the video.
         */
        public int videoHeight;
        /**
         * Video duration, in seconds.
         */
        public int videoDuration;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageVideo, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the result.
         * @param description A short description of the result, if known.
         * @param thumbnailUrl The URL of the video thumbnail (JPEG), if it exists.
         * @param videoUrl URL of the embedded video player or video file.
         * @param mimeType MIME type of the content of the video URL, only &quot;text/html&quot; or &quot;video/mp4&quot; are currently supported.
         * @param videoWidth Width of the video.
         * @param videoHeight Height of the video.
         * @param videoDuration Video duration, in seconds.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageVideo, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultVideo(String id, String title, String description, String thumbnailUrl, String videoUrl, String mimeType, int videoWidth, int videoHeight, int videoDuration, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.thumbnailUrl = thumbnailUrl;
            this.videoUrl = videoUrl;
            this.mimeType = mimeType;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoDuration = videoDuration;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1724073191;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1724073191;
        }
    }

    /**
     * Represents a link to an opus-encoded audio file within an OGG container, single channel audio.
     */
    public static class InputInlineQueryResultVoiceNote extends InputInlineQueryResult {
        /**
         * Unique identifier of the query result.
         */
        public String id;
        /**
         * Title of the voice note.
         */
        public String title;
        /**
         * The URL of the voice note file.
         */
        public String voiceNoteUrl;
        /**
         * Duration of the voice note, in seconds.
         */
        public int voiceNoteDuration;
        /**
         * The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageVoiceNote, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public InputInlineQueryResultVoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the query result.
         * @param title Title of the voice note.
         * @param voiceNoteUrl The URL of the voice note file.
         * @param voiceNoteDuration Duration of the voice note, in seconds.
         * @param replyMarkup The message reply markup. Must be of type replyMarkupInlineKeyboard or null.
         * @param inputMessageContent The content of the message to be sent. Must be one of the following types: InputMessageText, InputMessageVoiceNote, InputMessageLocation, InputMessageVenue or InputMessageContact.
         */
        public InputInlineQueryResultVoiceNote(String id, String title, String voiceNoteUrl, int voiceNoteDuration, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.id = id;
            this.title = title;
            this.voiceNoteUrl = voiceNoteUrl;
            this.voiceNoteDuration = voiceNoteDuration;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1790072503;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1790072503;
        }
    }

    /**
     * This class is an abstract base class.
     * The content of a message to send.
     */
    public abstract static class InputMessageContent extends Object {
    }

    /**
     * A text message.
     */
    public static class InputMessageText extends InputMessageContent {
        /**
         * Formatted text to be sent. Only Bold, Italic, Code, Pre, PreCode and TextUrl entities are allowed to be specified manually.
         */
        public FormattedText text;
        /**
         * True, if rich web page previews for URLs in the message text should be disabled.
         */
        public boolean disableWebPagePreview;
        /**
         * True, if a chat message draft should be deleted.
         */
        public boolean clearDraft;

        /**
         * Default constructor.
         */
        public InputMessageText() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Formatted text to be sent. Only Bold, Italic, Code, Pre, PreCode and TextUrl entities are allowed to be specified manually.
         * @param disableWebPagePreview True, if rich web page previews for URLs in the message text should be disabled.
         * @param clearDraft True, if a chat message draft should be deleted.
         */
        public InputMessageText(FormattedText text, boolean disableWebPagePreview, boolean clearDraft) {
            this.text = text;
            this.disableWebPagePreview = disableWebPagePreview;
            this.clearDraft = clearDraft;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 247050392;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 247050392;
        }
    }

    /**
     * An animation message (GIF-style).
     */
    public static class InputMessageAnimation extends InputMessageContent {
        /**
         * Animation file to be sent.
         */
        public InputFile animation;
        /**
         * Animation thumbnail, if available.
         */
        public InputThumbnail thumbnail;
        /**
         * Duration of the animation, in seconds.
         */
        public int duration;
        /**
         * Width of the animation; may be replaced by the server.
         */
        public int width;
        /**
         * Height of the animation; may be replaced by the server.
         */
        public int height;
        /**
         * Animation caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public InputMessageAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animation Animation file to be sent.
         * @param thumbnail Animation thumbnail, if available.
         * @param duration Duration of the animation, in seconds.
         * @param width Width of the animation; may be replaced by the server.
         * @param height Height of the animation; may be replaced by the server.
         * @param caption Animation caption; 0-200 characters.
         */
        public InputMessageAnimation(InputFile animation, InputThumbnail thumbnail, int duration, int width, int height, FormattedText caption) {
            this.animation = animation;
            this.thumbnail = thumbnail;
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 926542724;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 926542724;
        }
    }

    /**
     * An audio message.
     */
    public static class InputMessageAudio extends InputMessageContent {
        /**
         * Audio file to be sent.
         */
        public InputFile audio;
        /**
         * Thumbnail of the cover for the album, if available.
         */
        public InputThumbnail albumCoverThumbnail;
        /**
         * Duration of the audio, in seconds; may be replaced by the server.
         */
        public int duration;
        /**
         * Title of the audio; 0-64 characters; may be replaced by the server.
         */
        public String title;
        /**
         * Performer of the audio; 0-64 characters, may be replaced by the server.
         */
        public String performer;
        /**
         * Audio caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public InputMessageAudio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param audio Audio file to be sent.
         * @param albumCoverThumbnail Thumbnail of the cover for the album, if available.
         * @param duration Duration of the audio, in seconds; may be replaced by the server.
         * @param title Title of the audio; 0-64 characters; may be replaced by the server.
         * @param performer Performer of the audio; 0-64 characters, may be replaced by the server.
         * @param caption Audio caption; 0-200 characters.
         */
        public InputMessageAudio(InputFile audio, InputThumbnail albumCoverThumbnail, int duration, String title, String performer, FormattedText caption) {
            this.audio = audio;
            this.albumCoverThumbnail = albumCoverThumbnail;
            this.duration = duration;
            this.title = title;
            this.performer = performer;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -626786126;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -626786126;
        }
    }

    /**
     * A document message (general file).
     */
    public static class InputMessageDocument extends InputMessageContent {
        /**
         * Document to be sent.
         */
        public InputFile document;
        /**
         * Document thumbnail, if available.
         */
        public InputThumbnail thumbnail;
        /**
         * Document caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public InputMessageDocument() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param document Document to be sent.
         * @param thumbnail Document thumbnail, if available.
         * @param caption Document caption; 0-200 characters.
         */
        public InputMessageDocument(InputFile document, InputThumbnail thumbnail, FormattedText caption) {
            this.document = document;
            this.thumbnail = thumbnail;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 937970604;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 937970604;
        }
    }

    /**
     * A photo message.
     */
    public static class InputMessagePhoto extends InputMessageContent {
        /**
         * Photo to send.
         */
        public InputFile photo;
        /**
         * Photo thumbnail to be sent, this is sent to the other party in secret chats only.
         */
        public InputThumbnail thumbnail;
        /**
         * File identifiers of the stickers added to the photo, if applicable.
         */
        public int[] addedStickerFileIds;
        /**
         * Photo width.
         */
        public int width;
        /**
         * Photo height.
         */
        public int height;
        /**
         * Photo caption; 0-200 characters.
         */
        public FormattedText caption;
        /**
         * Photo TTL (Time To Live), in seconds (0-60). A non-zero TTL can be specified only in private chats.
         */
        public int ttl;

        /**
         * Default constructor.
         */
        public InputMessagePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param photo Photo to send.
         * @param thumbnail Photo thumbnail to be sent, this is sent to the other party in secret chats only.
         * @param addedStickerFileIds File identifiers of the stickers added to the photo, if applicable.
         * @param width Photo width.
         * @param height Photo height.
         * @param caption Photo caption; 0-200 characters.
         * @param ttl Photo TTL (Time To Live), in seconds (0-60). A non-zero TTL can be specified only in private chats.
         */
        public InputMessagePhoto(InputFile photo, InputThumbnail thumbnail, int[] addedStickerFileIds, int width, int height, FormattedText caption, int ttl) {
            this.photo = photo;
            this.thumbnail = thumbnail;
            this.addedStickerFileIds = addedStickerFileIds;
            this.width = width;
            this.height = height;
            this.caption = caption;
            this.ttl = ttl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1648801584;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1648801584;
        }
    }

    /**
     * A sticker message.
     */
    public static class InputMessageSticker extends InputMessageContent {
        /**
         * Sticker to be sent.
         */
        public InputFile sticker;
        /**
         * Sticker thumbnail, if available.
         */
        public InputThumbnail thumbnail;
        /**
         * Sticker width.
         */
        public int width;
        /**
         * Sticker height.
         */
        public int height;

        /**
         * Default constructor.
         */
        public InputMessageSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker to be sent.
         * @param thumbnail Sticker thumbnail, if available.
         * @param width Sticker width.
         * @param height Sticker height.
         */
        public InputMessageSticker(InputFile sticker, InputThumbnail thumbnail, int width, int height) {
            this.sticker = sticker;
            this.thumbnail = thumbnail;
            this.width = width;
            this.height = height;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 740776325;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 740776325;
        }
    }

    /**
     * A video message.
     */
    public static class InputMessageVideo extends InputMessageContent {
        /**
         * Video to be sent.
         */
        public InputFile video;
        /**
         * Video thumbnail, if available.
         */
        public InputThumbnail thumbnail;
        /**
         * File identifiers of the stickers added to the video, if applicable.
         */
        public int[] addedStickerFileIds;
        /**
         * Duration of the video, in seconds.
         */
        public int duration;
        /**
         * Video width.
         */
        public int width;
        /**
         * Video height.
         */
        public int height;
        /**
         * True, if the video should be tried to be streamed.
         */
        public boolean supportsStreaming;
        /**
         * Video caption; 0-200 characters.
         */
        public FormattedText caption;
        /**
         * Video TTL (Time To Live), in seconds (0-60). A non-zero TTL can be specified only in private chats.
         */
        public int ttl;

        /**
         * Default constructor.
         */
        public InputMessageVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param video Video to be sent.
         * @param thumbnail Video thumbnail, if available.
         * @param addedStickerFileIds File identifiers of the stickers added to the video, if applicable.
         * @param duration Duration of the video, in seconds.
         * @param width Video width.
         * @param height Video height.
         * @param supportsStreaming True, if the video should be tried to be streamed.
         * @param caption Video caption; 0-200 characters.
         * @param ttl Video TTL (Time To Live), in seconds (0-60). A non-zero TTL can be specified only in private chats.
         */
        public InputMessageVideo(InputFile video, InputThumbnail thumbnail, int[] addedStickerFileIds, int duration, int width, int height, boolean supportsStreaming, FormattedText caption, int ttl) {
            this.video = video;
            this.thumbnail = thumbnail;
            this.addedStickerFileIds = addedStickerFileIds;
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.supportsStreaming = supportsStreaming;
            this.caption = caption;
            this.ttl = ttl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2108486755;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2108486755;
        }
    }

    /**
     * A video note message.
     */
    public static class InputMessageVideoNote extends InputMessageContent {
        /**
         * Video note to be sent.
         */
        public InputFile videoNote;
        /**
         * Video thumbnail, if available.
         */
        public InputThumbnail thumbnail;
        /**
         * Duration of the video, in seconds.
         */
        public int duration;
        /**
         * Video width and height; must be positive and not greater than 640.
         */
        public int length;

        /**
         * Default constructor.
         */
        public InputMessageVideoNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param videoNote Video note to be sent.
         * @param thumbnail Video thumbnail, if available.
         * @param duration Duration of the video, in seconds.
         * @param length Video width and height; must be positive and not greater than 640.
         */
        public InputMessageVideoNote(InputFile videoNote, InputThumbnail thumbnail, int duration, int length) {
            this.videoNote = videoNote;
            this.thumbnail = thumbnail;
            this.duration = duration;
            this.length = length;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 279108859;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 279108859;
        }
    }

    /**
     * A voice note message.
     */
    public static class InputMessageVoiceNote extends InputMessageContent {
        /**
         * Voice note to be sent.
         */
        public InputFile voiceNote;
        /**
         * Duration of the voice note, in seconds.
         */
        public int duration;
        /**
         * Waveform representation of the voice note, in 5-bit format.
         */
        public byte[] waveform;
        /**
         * Voice note caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public InputMessageVoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param voiceNote Voice note to be sent.
         * @param duration Duration of the voice note, in seconds.
         * @param waveform Waveform representation of the voice note, in 5-bit format.
         * @param caption Voice note caption; 0-200 characters.
         */
        public InputMessageVoiceNote(InputFile voiceNote, int duration, byte[] waveform, FormattedText caption) {
            this.voiceNote = voiceNote;
            this.duration = duration;
            this.waveform = waveform;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2136519657;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2136519657;
        }
    }

    /**
     * A message with a location.
     */
    public static class InputMessageLocation extends InputMessageContent {
        /**
         * Location to be sent.
         */
        public Location location;
        /**
         * Period for which the location can be updated, in seconds; should bebetween 60 and 86400 for a live location and 0 otherwise.
         */
        public int livePeriod;

        /**
         * Default constructor.
         */
        public InputMessageLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param location Location to be sent.
         * @param livePeriod Period for which the location can be updated, in seconds; should bebetween 60 and 86400 for a live location and 0 otherwise.
         */
        public InputMessageLocation(Location location, int livePeriod) {
            this.location = location;
            this.livePeriod = livePeriod;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1624179655;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1624179655;
        }
    }

    /**
     * A message with information about a venue.
     */
    public static class InputMessageVenue extends InputMessageContent {
        /**
         * Venue to send.
         */
        public Venue venue;

        /**
         * Default constructor.
         */
        public InputMessageVenue() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param venue Venue to send.
         */
        public InputMessageVenue(Venue venue) {
            this.venue = venue;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1447926269;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1447926269;
        }
    }

    /**
     * A message containing a user contact.
     */
    public static class InputMessageContact extends InputMessageContent {
        /**
         * Contact to send.
         */
        public Contact contact;

        /**
         * Default constructor.
         */
        public InputMessageContact() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param contact Contact to send.
         */
        public InputMessageContact(Contact contact) {
            this.contact = contact;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -982446849;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -982446849;
        }
    }

    /**
     * A message with a game; not supported for channels or secret chats.
     */
    public static class InputMessageGame extends InputMessageContent {
        /**
         * User identifier of the bot that owns the game.
         */
        public int botUserId;
        /**
         * Short name of the game.
         */
        public String gameShortName;

        /**
         * Default constructor.
         */
        public InputMessageGame() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param botUserId User identifier of the bot that owns the game.
         * @param gameShortName Short name of the game.
         */
        public InputMessageGame(int botUserId, String gameShortName) {
            this.botUserId = botUserId;
            this.gameShortName = gameShortName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1728000914;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1728000914;
        }
    }

    /**
     * A message with an invoice; can be used only by bots and only in private chats.
     */
    public static class InputMessageInvoice extends InputMessageContent {
        /**
         * Invoice.
         */
        public Invoice invoice;
        /**
         * Product title; 1-32 characters.
         */
        public String title;
        /**
         * Product description; 0-255 characters.
         */
        public String description;
        /**
         * Product photo URL; optional.
         */
        public String photoUrl;
        /**
         * Product photo size.
         */
        public int photoSize;
        /**
         * Product photo width.
         */
        public int photoWidth;
        /**
         * Product photo height.
         */
        public int photoHeight;
        /**
         * The invoice payload.
         */
        public byte[] payload;
        /**
         * Payment provider token.
         */
        public String providerToken;
        /**
         * JSON-encoded data about the invoice, which will be shared with the payment provider.
         */
        public String providerData;
        /**
         * Unique invoice bot startParameter for the generation of this invoice.
         */
        public String startParameter;

        /**
         * Default constructor.
         */
        public InputMessageInvoice() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param invoice Invoice.
         * @param title Product title; 1-32 characters.
         * @param description Product description; 0-255 characters.
         * @param photoUrl Product photo URL; optional.
         * @param photoSize Product photo size.
         * @param photoWidth Product photo width.
         * @param photoHeight Product photo height.
         * @param payload The invoice payload.
         * @param providerToken Payment provider token.
         * @param providerData JSON-encoded data about the invoice, which will be shared with the payment provider.
         * @param startParameter Unique invoice bot startParameter for the generation of this invoice.
         */
        public InputMessageInvoice(Invoice invoice, String title, String description, String photoUrl, int photoSize, int photoWidth, int photoHeight, byte[] payload, String providerToken, String providerData, String startParameter) {
            this.invoice = invoice;
            this.title = title;
            this.description = description;
            this.photoUrl = photoUrl;
            this.photoSize = photoSize;
            this.photoWidth = photoWidth;
            this.photoHeight = photoHeight;
            this.payload = payload;
            this.providerToken = providerToken;
            this.providerData = providerData;
            this.startParameter = startParameter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1038812175;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1038812175;
        }
    }

    /**
     * A forwarded message.
     */
    public static class InputMessageForwarded extends InputMessageContent {
        /**
         * Identifier for the chat this forwarded message came from.
         */
        public long fromChatId;
        /**
         * Identifier of the message to forward.
         */
        public long messageId;
        /**
         * True, if a game message should be shared within a launched game; applies only to game messages.
         */
        public boolean inGameShare;

        /**
         * Default constructor.
         */
        public InputMessageForwarded() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fromChatId Identifier for the chat this forwarded message came from.
         * @param messageId Identifier of the message to forward.
         * @param inGameShare True, if a game message should be shared within a launched game; applies only to game messages.
         */
        public InputMessageForwarded(long fromChatId, long messageId, boolean inGameShare) {
            this.fromChatId = fromChatId;
            this.messageId = messageId;
            this.inGameShare = inGameShare;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1561363198;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1561363198;
        }
    }

    /**
     * Describes a sticker that should be added to a sticker set.
     */
    public static class InputSticker extends Object {
        /**
         * PNG image with the sticker; must be up to 512 kB in size and fit in a 512x512 square.
         */
        public InputFile pngSticker;
        /**
         * Emoji corresponding to the sticker.
         */
        public String emojis;
        /**
         * For masks, position where the mask should be placed; may be null.
         */
        public @Nullable MaskPosition maskPosition;

        /**
         * Default constructor.
         */
        public InputSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pngSticker PNG image with the sticker; must be up to 512 kB in size and fit in a 512x512 square.
         * @param emojis Emoji corresponding to the sticker.
         * @param maskPosition For masks, position where the mask should be placed; may be null.
         */
        public InputSticker(InputFile pngSticker, String emojis, MaskPosition maskPosition) {
            this.pngSticker = pngSticker;
            this.emojis = emojis;
            this.maskPosition = maskPosition;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1998602205;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1998602205;
        }
    }

    /**
     * A thumbnail to be sent along with a file; should be in JPEG or WEBP format for stickers, and less than 200 kB in size.
     */
    public static class InputThumbnail extends Object {
        /**
         * Thumbnail file to send. Sending thumbnails by fileId is currently not supported.
         */
        public InputFile thumbnail;
        /**
         * Thumbnail width, usually shouldn't exceed 90. Use 0 if unknown.
         */
        public int width;
        /**
         * Thumbnail height, usually shouldn't exceed 90. Use 0 if unknown.
         */
        public int height;

        /**
         * Default constructor.
         */
        public InputThumbnail() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param thumbnail Thumbnail file to send. Sending thumbnails by fileId is currently not supported.
         * @param width Thumbnail width, usually shouldn't exceed 90. Use 0 if unknown.
         * @param height Thumbnail height, usually shouldn't exceed 90. Use 0 if unknown.
         */
        public InputThumbnail(InputFile thumbnail, int width, int height) {
            this.thumbnail = thumbnail;
            this.width = width;
            this.height = height;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1582387236;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1582387236;
        }
    }

    /**
     * Product invoice.
     */
    public static class Invoice extends Object {
        /**
         * ISO 4217 currency code.
         */
        public String currency;
        /**
         * A list of objects used to calculate the total price of the product.
         */
        public LabeledPricePart[] priceParts;
        /**
         * True, if the payment is a test payment.
         */
        public boolean isTest;
        /**
         * True, if the user's name is needed for payment.
         */
        public boolean needName;
        /**
         * True, if the user's phone number is needed for payment.
         */
        public boolean needPhoneNumber;
        /**
         * True, if the user's email address is needed for payment.
         */
        public boolean needEmailAddress;
        /**
         * True, if the user's shipping address is needed for payment.
         */
        public boolean needShippingAddress;
        /**
         * True, if the user's phone number will be sent to the provider.
         */
        public boolean sendPhoneNumberToProvider;
        /**
         * True, if the user's email address will be sent to the provider.
         */
        public boolean sendEmailAddressToProvider;
        /**
         * True, if the total price depends on the shipping method.
         */
        public boolean isFlexible;

        /**
         * Default constructor.
         */
        public Invoice() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param currency ISO 4217 currency code.
         * @param priceParts A list of objects used to calculate the total price of the product.
         * @param isTest True, if the payment is a test payment.
         * @param needName True, if the user's name is needed for payment.
         * @param needPhoneNumber True, if the user's phone number is needed for payment.
         * @param needEmailAddress True, if the user's email address is needed for payment.
         * @param needShippingAddress True, if the user's shipping address is needed for payment.
         * @param sendPhoneNumberToProvider True, if the user's phone number will be sent to the provider.
         * @param sendEmailAddressToProvider True, if the user's email address will be sent to the provider.
         * @param isFlexible True, if the total price depends on the shipping method.
         */
        public Invoice(String currency, LabeledPricePart[] priceParts, boolean isTest, boolean needName, boolean needPhoneNumber, boolean needEmailAddress, boolean needShippingAddress, boolean sendPhoneNumberToProvider, boolean sendEmailAddressToProvider, boolean isFlexible) {
            this.currency = currency;
            this.priceParts = priceParts;
            this.isTest = isTest;
            this.needName = needName;
            this.needPhoneNumber = needPhoneNumber;
            this.needEmailAddress = needEmailAddress;
            this.needShippingAddress = needShippingAddress;
            this.sendPhoneNumberToProvider = sendPhoneNumberToProvider;
            this.sendEmailAddressToProvider = sendEmailAddressToProvider;
            this.isFlexible = isFlexible;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -368451690;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -368451690;
        }
    }

    /**
     * Represents a single button in a bot keyboard.
     */
    public static class KeyboardButton extends Object {
        /**
         * Text of the button.
         */
        public String text;
        /**
         * Type of the button.
         */
        public KeyboardButtonType type;

        /**
         * Default constructor.
         */
        public KeyboardButton() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text of the button.
         * @param type Type of the button.
         */
        public KeyboardButton(String text, KeyboardButtonType type) {
            this.text = text;
            this.type = type;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2069836172;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2069836172;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes a keyboard button type.
     */
    public abstract static class KeyboardButtonType extends Object {
    }

    /**
     * A simple button, with text that should be sent when the button is pressed.
     */
    public static class KeyboardButtonTypeText extends KeyboardButtonType {

        /**
         * Default constructor.
         */
        public KeyboardButtonTypeText() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1773037256;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1773037256;
        }
    }

    /**
     * A button that sends the user's phone number when pressed; available only in private chats.
     */
    public static class KeyboardButtonTypeRequestPhoneNumber extends KeyboardButtonType {

        /**
         * Default constructor.
         */
        public KeyboardButtonTypeRequestPhoneNumber() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1529235527;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1529235527;
        }
    }

    /**
     * A button that sends the user's location when pressed; available only in private chats.
     */
    public static class KeyboardButtonTypeRequestLocation extends KeyboardButtonType {

        /**
         * Default constructor.
         */
        public KeyboardButtonTypeRequestLocation() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -125661955;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -125661955;
        }
    }

    /**
     * Portion of the price of a product (e.g., &quot;delivery cost&quot;, &quot;tax amount&quot;).
     */
    public static class LabeledPricePart extends Object {
        /**
         * Label for this portion of the product price.
         */
        public String label;
        /**
         * Currency amount in minimal quantity of the currency.
         */
        public long amount;

        /**
         * Default constructor.
         */
        public LabeledPricePart() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param label Label for this portion of the product price.
         * @param amount Currency amount in minimal quantity of the currency.
         */
        public LabeledPricePart(String label, long amount) {
            this.label = label;
            this.amount = amount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 552789798;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 552789798;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the relationship between user A and user B. For incoming_link, user A is the current user; for outgoing_link, user B is the current user.
     */
    public abstract static class LinkState extends Object {
    }

    /**
     * The phone number of user A is not known to user B.
     */
    public static class LinkStateNone extends LinkState {

        /**
         * Default constructor.
         */
        public LinkStateNone() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 951430287;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 951430287;
        }
    }

    /**
     * The phone number of user A is known but that number has not been saved to the contacts list of user B.
     */
    public static class LinkStateKnowsPhoneNumber extends LinkState {

        /**
         * Default constructor.
         */
        public LinkStateKnowsPhoneNumber() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 380898199;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 380898199;
        }
    }

    /**
     * The phone number of user A has been saved to the contacts list of user B.
     */
    public static class LinkStateIsContact extends LinkState {

        /**
         * Default constructor.
         */
        public LinkStateIsContact() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1000499465;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1000499465;
        }
    }

    /**
     * Represents a local file.
     */
    public static class LocalFile extends Object {
        /**
         * Local path to the locally available file part; may be empty.
         */
        public String path;
        /**
         * True, if it is possible to try to download or generate the file.
         */
        public boolean canBeDownloaded;
        /**
         * True, if the file can be deleted.
         */
        public boolean canBeDeleted;
        /**
         * True, if the file is currently being downloaded (or a local copy is being generated by some other means).
         */
        public boolean isDownloadingActive;
        /**
         * True, if the local copy is fully available.
         */
        public boolean isDownloadingCompleted;
        /**
         * If isDownloadingCompleted is false, then only some prefix of the file is ready to be read. downloadedPrefixSize is the size of that prefix.
         */
        public int downloadedPrefixSize;
        /**
         * Total downloaded file bytes. Should be used only for calculating download progress. The actual file size may be bigger, and some parts of it may contain garbage.
         */
        public int downloadedSize;

        /**
         * Default constructor.
         */
        public LocalFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param path Local path to the locally available file part; may be empty.
         * @param canBeDownloaded True, if it is possible to try to download or generate the file.
         * @param canBeDeleted True, if the file can be deleted.
         * @param isDownloadingActive True, if the file is currently being downloaded (or a local copy is being generated by some other means).
         * @param isDownloadingCompleted True, if the local copy is fully available.
         * @param downloadedPrefixSize If isDownloadingCompleted is false, then only some prefix of the file is ready to be read. downloadedPrefixSize is the size of that prefix.
         * @param downloadedSize Total downloaded file bytes. Should be used only for calculating download progress. The actual file size may be bigger, and some parts of it may contain garbage.
         */
        public LocalFile(String path, boolean canBeDownloaded, boolean canBeDeleted, boolean isDownloadingActive, boolean isDownloadingCompleted, int downloadedPrefixSize, int downloadedSize) {
            this.path = path;
            this.canBeDownloaded = canBeDownloaded;
            this.canBeDeleted = canBeDeleted;
            this.isDownloadingActive = isDownloadingActive;
            this.isDownloadingCompleted = isDownloadingCompleted;
            this.downloadedPrefixSize = downloadedPrefixSize;
            this.downloadedSize = downloadedSize;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 847939462;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 847939462;
        }
    }

    /**
     * Describes a location on planet Earth.
     */
    public static class Location extends Object {
        /**
         * Latitude of the location in degrees; as defined by the sender.
         */
        public double latitude;
        /**
         * Longitude of the location, in degrees; as defined by the sender.
         */
        public double longitude;

        /**
         * Default constructor.
         */
        public Location() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param latitude Latitude of the location in degrees; as defined by the sender.
         * @param longitude Longitude of the location, in degrees; as defined by the sender.
         */
        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 749028016;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 749028016;
        }
    }

    /**
     * This class is an abstract base class.
     * Part of the face, relative to which a mask should be placed.
     */
    public abstract static class MaskPoint extends Object {
    }

    /**
     * A mask should be placed relatively to the forehead.
     */
    public static class MaskPointForehead extends MaskPoint {

        /**
         * Default constructor.
         */
        public MaskPointForehead() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1027512005;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1027512005;
        }
    }

    /**
     * A mask should be placed relatively to the eyes.
     */
    public static class MaskPointEyes extends MaskPoint {

        /**
         * Default constructor.
         */
        public MaskPointEyes() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1748310861;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1748310861;
        }
    }

    /**
     * A mask should be placed relatively to the mouth.
     */
    public static class MaskPointMouth extends MaskPoint {

        /**
         * Default constructor.
         */
        public MaskPointMouth() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 411773406;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 411773406;
        }
    }

    /**
     * A mask should be placed relatively to the chin.
     */
    public static class MaskPointChin extends MaskPoint {

        /**
         * Default constructor.
         */
        public MaskPointChin() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 534995335;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 534995335;
        }
    }

    /**
     * Position on a photo where a mask should be placed.
     */
    public static class MaskPosition extends Object {
        /**
         * Part of the face, relative to which the mask should be placed.
         */
        public MaskPoint point;
        /**
         * Shift by X-axis measured in widths of the mask scaled to the face size, from left to right. (For example, -1.0 will place the mask just to the left of the default mask position.)
         */
        public double xShift;
        /**
         * Shift by Y-axis measured in heights of the mask scaled to the face size, from top to bottom. (For example, 1.0 will place the mask just below the default mask position.)
         */
        public double yShift;
        /**
         * Mask scaling coefficient. (For example, 2.0 means a doubled size.)
         */
        public double scale;

        /**
         * Default constructor.
         */
        public MaskPosition() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param point Part of the face, relative to which the mask should be placed.
         * @param xShift Shift by X-axis measured in widths of the mask scaled to the face size, from left to right. (For example, -1.0 will place the mask just to the left of the default mask position.)
         * @param yShift Shift by Y-axis measured in heights of the mask scaled to the face size, from top to bottom. (For example, 1.0 will place the mask just below the default mask position.)
         * @param scale Mask scaling coefficient. (For example, 2.0 means a doubled size.)
         */
        public MaskPosition(MaskPoint point, double xShift, double yShift, double scale) {
            this.point = point;
            this.xShift = xShift;
            this.yShift = yShift;
            this.scale = scale;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2097433026;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2097433026;
        }
    }

    /**
     * Describes a message.
     */
    public static class Message extends Object {
        /**
         * Unique message identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the message; 0 if unknown. It is unknown for channel posts.
         */
        public int senderUserId;
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Information about the sending state of the message; may be null.
         */
        public @Nullable MessageSendingState sendingState;
        /**
         * True, if the message is outgoing.
         */
        public boolean isOutgoing;
        /**
         * True, if the message can be edited.
         */
        public boolean canBeEdited;
        /**
         * True, if the message can be forwarded.
         */
        public boolean canBeForwarded;
        /**
         * True, if the message can be deleted only for the current user while other users will continue to see it.
         */
        public boolean canBeDeletedOnlyForSelf;
        /**
         * True, if the message can be deleted for all users.
         */
        public boolean canBeDeletedForAllUsers;
        /**
         * True, if the message is a channel post. All messages to channels are channel posts, all other messages are not channel posts.
         */
        public boolean isChannelPost;
        /**
         * True, if the message contains an unread mention for the current user.
         */
        public boolean containsUnreadMention;
        /**
         * Point in time (Unix timestamp) when the message was sent.
         */
        public int date;
        /**
         * Point in time (Unix timestamp) when the message was last edited.
         */
        public int editDate;
        /**
         * Information about the initial message sender; may be null.
         */
        public @Nullable MessageForwardInfo forwardInfo;
        /**
         * If non-zero, the identifier of the message this message is replying to; can be the identifier of a deleted message.
         */
        public long replyToMessageId;
        /**
         * For self-destructing messages, the message's TTL (Time To Live), in seconds; 0 if none. TDLib will send updateDeleteMessages or updateMessageContent once the TTL expires.
         */
        public int ttl;
        /**
         * Time left before the message expires, in seconds.
         */
        public double ttlExpiresIn;
        /**
         * If non-zero, the user identifier of the bot through which this message was sent.
         */
        public int viaBotUserId;
        /**
         * For channel posts, optional author signature.
         */
        public String authorSignature;
        /**
         * Number of times this message was viewed.
         */
        public int views;
        /**
         * Unique identifier of an album this message belongs to. Only photos and videos can be grouped together in albums.
         */
        public long mediaAlbumId;
        /**
         * Content of the message.
         */
        public MessageContent content;
        /**
         * Reply markup for the message; may be null.
         */
        public @Nullable ReplyMarkup replyMarkup;

        /**
         * Default constructor.
         */
        public Message() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique message identifier.
         * @param senderUserId Identifier of the user who sent the message; 0 if unknown. It is unknown for channel posts.
         * @param chatId Chat identifier.
         * @param sendingState Information about the sending state of the message; may be null.
         * @param isOutgoing True, if the message is outgoing.
         * @param canBeEdited True, if the message can be edited.
         * @param canBeForwarded True, if the message can be forwarded.
         * @param canBeDeletedOnlyForSelf True, if the message can be deleted only for the current user while other users will continue to see it.
         * @param canBeDeletedForAllUsers True, if the message can be deleted for all users.
         * @param isChannelPost True, if the message is a channel post. All messages to channels are channel posts, all other messages are not channel posts.
         * @param containsUnreadMention True, if the message contains an unread mention for the current user.
         * @param date Point in time (Unix timestamp) when the message was sent.
         * @param editDate Point in time (Unix timestamp) when the message was last edited.
         * @param forwardInfo Information about the initial message sender; may be null.
         * @param replyToMessageId If non-zero, the identifier of the message this message is replying to; can be the identifier of a deleted message.
         * @param ttl For self-destructing messages, the message's TTL (Time To Live), in seconds; 0 if none. TDLib will send updateDeleteMessages or updateMessageContent once the TTL expires.
         * @param ttlExpiresIn Time left before the message expires, in seconds.
         * @param viaBotUserId If non-zero, the user identifier of the bot through which this message was sent.
         * @param authorSignature For channel posts, optional author signature.
         * @param views Number of times this message was viewed.
         * @param mediaAlbumId Unique identifier of an album this message belongs to. Only photos and videos can be grouped together in albums.
         * @param content Content of the message.
         * @param replyMarkup Reply markup for the message; may be null.
         */
        public Message(long id, int senderUserId, long chatId, MessageSendingState sendingState, boolean isOutgoing, boolean canBeEdited, boolean canBeForwarded, boolean canBeDeletedOnlyForSelf, boolean canBeDeletedForAllUsers, boolean isChannelPost, boolean containsUnreadMention, int date, int editDate, MessageForwardInfo forwardInfo, long replyToMessageId, int ttl, double ttlExpiresIn, int viaBotUserId, String authorSignature, int views, long mediaAlbumId, MessageContent content, ReplyMarkup replyMarkup) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.chatId = chatId;
            this.sendingState = sendingState;
            this.isOutgoing = isOutgoing;
            this.canBeEdited = canBeEdited;
            this.canBeForwarded = canBeForwarded;
            this.canBeDeletedOnlyForSelf = canBeDeletedOnlyForSelf;
            this.canBeDeletedForAllUsers = canBeDeletedForAllUsers;
            this.isChannelPost = isChannelPost;
            this.containsUnreadMention = containsUnreadMention;
            this.date = date;
            this.editDate = editDate;
            this.forwardInfo = forwardInfo;
            this.replyToMessageId = replyToMessageId;
            this.ttl = ttl;
            this.ttlExpiresIn = ttlExpiresIn;
            this.viaBotUserId = viaBotUserId;
            this.authorSignature = authorSignature;
            this.views = views;
            this.mediaAlbumId = mediaAlbumId;
            this.content = content;
            this.replyMarkup = replyMarkup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -675737627;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -675737627;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains the content of a message.
     */
    public abstract static class MessageContent extends Object {
    }

    /**
     * A text message.
     */
    public static class MessageText extends MessageContent {
        /**
         * Text of the message.
         */
        public FormattedText text;
        /**
         * A preview of the web page that's mentioned in the text; may be null.
         */
        public @Nullable WebPage webPage;

        /**
         * Default constructor.
         */
        public MessageText() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text of the message.
         * @param webPage A preview of the web page that's mentioned in the text; may be null.
         */
        public MessageText(FormattedText text, WebPage webPage) {
            this.text = text;
            this.webPage = webPage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1989037971;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1989037971;
        }
    }

    /**
     * An animation message (GIF-style).
     */
    public static class MessageAnimation extends MessageContent {
        /**
         * Message content.
         */
        public Animation animation;
        /**
         * Animation caption.
         */
        public FormattedText caption;
        /**
         * True, if the animation thumbnail must be blurred and the animation must be shown only while tapped.
         */
        public boolean isSecret;

        /**
         * Default constructor.
         */
        public MessageAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animation Message content.
         * @param caption Animation caption.
         * @param isSecret True, if the animation thumbnail must be blurred and the animation must be shown only while tapped.
         */
        public MessageAnimation(Animation animation, FormattedText caption, boolean isSecret) {
            this.animation = animation;
            this.caption = caption;
            this.isSecret = isSecret;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1306939396;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1306939396;
        }
    }

    /**
     * An audio message.
     */
    public static class MessageAudio extends MessageContent {
        /**
         * Message content.
         */
        public Audio audio;
        /**
         * Audio caption.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public MessageAudio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param audio Message content.
         * @param caption Audio caption.
         */
        public MessageAudio(Audio audio, FormattedText caption) {
            this.audio = audio;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 276722716;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 276722716;
        }
    }

    /**
     * A document message (general file).
     */
    public static class MessageDocument extends MessageContent {
        /**
         * Message content.
         */
        public Document document;
        /**
         * Document caption.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public MessageDocument() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param document Message content.
         * @param caption Document caption.
         */
        public MessageDocument(Document document, FormattedText caption) {
            this.document = document;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 596945783;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 596945783;
        }
    }

    /**
     * A photo message.
     */
    public static class MessagePhoto extends MessageContent {
        /**
         * Message content.
         */
        public Photo photo;
        /**
         * Photo caption.
         */
        public FormattedText caption;
        /**
         * True, if the photo must be blurred and must be shown only while tapped.
         */
        public boolean isSecret;

        /**
         * Default constructor.
         */
        public MessagePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param photo Message content.
         * @param caption Photo caption.
         * @param isSecret True, if the photo must be blurred and must be shown only while tapped.
         */
        public MessagePhoto(Photo photo, FormattedText caption, boolean isSecret) {
            this.photo = photo;
            this.caption = caption;
            this.isSecret = isSecret;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1851395174;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1851395174;
        }
    }

    /**
     * An expired photo message (self-destructed after TTL has elapsed).
     */
    public static class MessageExpiredPhoto extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageExpiredPhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1404641801;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1404641801;
        }
    }

    /**
     * A sticker message.
     */
    public static class MessageSticker extends MessageContent {
        /**
         * Message content.
         */
        public Sticker sticker;

        /**
         * Default constructor.
         */
        public MessageSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Message content.
         */
        public MessageSticker(Sticker sticker) {
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1779022878;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1779022878;
        }
    }

    /**
     * A video message.
     */
    public static class MessageVideo extends MessageContent {
        /**
         * Message content.
         */
        public Video video;
        /**
         * Video caption.
         */
        public FormattedText caption;
        /**
         * True, if the video thumbnail must be blurred and the video must be shown only while tapped.
         */
        public boolean isSecret;

        /**
         * Default constructor.
         */
        public MessageVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param video Message content.
         * @param caption Video caption.
         * @param isSecret True, if the video thumbnail must be blurred and the video must be shown only while tapped.
         */
        public MessageVideo(Video video, FormattedText caption, boolean isSecret) {
            this.video = video;
            this.caption = caption;
            this.isSecret = isSecret;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2021281344;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2021281344;
        }
    }

    /**
     * An expired video message (self-destructed after TTL has elapsed).
     */
    public static class MessageExpiredVideo extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageExpiredVideo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1212209981;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1212209981;
        }
    }

    /**
     * A video note message.
     */
    public static class MessageVideoNote extends MessageContent {
        /**
         * Message content.
         */
        public VideoNote videoNote;
        /**
         * True, if at least one of the recipients has viewed the video note.
         */
        public boolean isViewed;
        /**
         * True, if the video note thumbnail must be blurred and the video note must be shown only while tapped.
         */
        public boolean isSecret;

        /**
         * Default constructor.
         */
        public MessageVideoNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param videoNote Message content.
         * @param isViewed True, if at least one of the recipients has viewed the video note.
         * @param isSecret True, if the video note thumbnail must be blurred and the video note must be shown only while tapped.
         */
        public MessageVideoNote(VideoNote videoNote, boolean isViewed, boolean isSecret) {
            this.videoNote = videoNote;
            this.isViewed = isViewed;
            this.isSecret = isSecret;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 963323014;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 963323014;
        }
    }

    /**
     * A voice note message.
     */
    public static class MessageVoiceNote extends MessageContent {
        /**
         * Message content.
         */
        public VoiceNote voiceNote;
        /**
         * Voice note caption.
         */
        public FormattedText caption;
        /**
         * True, if at least one of the recipients has listened to the voice note.
         */
        public boolean isListened;

        /**
         * Default constructor.
         */
        public MessageVoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param voiceNote Message content.
         * @param caption Voice note caption.
         * @param isListened True, if at least one of the recipients has listened to the voice note.
         */
        public MessageVoiceNote(VoiceNote voiceNote, FormattedText caption, boolean isListened) {
            this.voiceNote = voiceNote;
            this.caption = caption;
            this.isListened = isListened;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 527777781;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 527777781;
        }
    }

    /**
     * A message with a location.
     */
    public static class MessageLocation extends MessageContent {
        /**
         * Message content.
         */
        public Location location;
        /**
         * Time relative to the message sent date until which the location can be updated, in seconds.
         */
        public int livePeriod;
        /**
         * Left time for which the location can be updated, in seconds. updateMessageContent is not sent when this field changes.
         */
        public int expiresIn;

        /**
         * Default constructor.
         */
        public MessageLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param location Message content.
         * @param livePeriod Time relative to the message sent date until which the location can be updated, in seconds.
         * @param expiresIn Left time for which the location can be updated, in seconds. updateMessageContent is not sent when this field changes.
         */
        public MessageLocation(Location location, int livePeriod, int expiresIn) {
            this.location = location;
            this.livePeriod = livePeriod;
            this.expiresIn = expiresIn;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1301887786;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1301887786;
        }
    }

    /**
     * A message with information about a venue.
     */
    public static class MessageVenue extends MessageContent {
        /**
         * Message content.
         */
        public Venue venue;

        /**
         * Default constructor.
         */
        public MessageVenue() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param venue Message content.
         */
        public MessageVenue(Venue venue) {
            this.venue = venue;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2146492043;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2146492043;
        }
    }

    /**
     * A message with a user contact.
     */
    public static class MessageContact extends MessageContent {
        /**
         * Message content.
         */
        public Contact contact;

        /**
         * Default constructor.
         */
        public MessageContact() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param contact Message content.
         */
        public MessageContact(Contact contact) {
            this.contact = contact;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -512684966;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -512684966;
        }
    }

    /**
     * A message with a game.
     */
    public static class MessageGame extends MessageContent {
        /**
         * Game.
         */
        public Game game;

        /**
         * Default constructor.
         */
        public MessageGame() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param game Game.
         */
        public MessageGame(Game game) {
            this.game = game;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -69441162;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -69441162;
        }
    }

    /**
     * A message with an invoice from a bot.
     */
    public static class MessageInvoice extends MessageContent {
        /**
         * Product title.
         */
        public String title;
        /**
         * Product description.
         */
        public String description;
        /**
         * Product photo; may be null.
         */
        public @Nullable Photo photo;
        /**
         * Currency for the product price.
         */
        public String currency;
        /**
         * Product total price in the minimal quantity of the currency.
         */
        public long totalAmount;
        /**
         * Unique invoice bot startParameter. To share an invoice use the URL https://t.me/{botUsername}?start={startParameter}.
         */
        public String startParameter;
        /**
         * True, if the invoice is a test invoice.
         */
        public boolean isTest;
        /**
         * True, if the shipping address should be specified.
         */
        public boolean needShippingAddress;
        /**
         * The identifier of the message with the receipt, after the product has been purchased.
         */
        public long receiptMessageId;

        /**
         * Default constructor.
         */
        public MessageInvoice() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Product title.
         * @param description Product description.
         * @param photo Product photo; may be null.
         * @param currency Currency for the product price.
         * @param totalAmount Product total price in the minimal quantity of the currency.
         * @param startParameter Unique invoice bot startParameter. To share an invoice use the URL https://t.me/{botUsername}?start={startParameter}.
         * @param isTest True, if the invoice is a test invoice.
         * @param needShippingAddress True, if the shipping address should be specified.
         * @param receiptMessageId The identifier of the message with the receipt, after the product has been purchased.
         */
        public MessageInvoice(String title, String description, Photo photo, String currency, long totalAmount, String startParameter, boolean isTest, boolean needShippingAddress, long receiptMessageId) {
            this.title = title;
            this.description = description;
            this.photo = photo;
            this.currency = currency;
            this.totalAmount = totalAmount;
            this.startParameter = startParameter;
            this.isTest = isTest;
            this.needShippingAddress = needShippingAddress;
            this.receiptMessageId = receiptMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1916671476;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1916671476;
        }
    }

    /**
     * A message with information about an ended call.
     */
    public static class MessageCall extends MessageContent {
        /**
         * Reason why the call was discarded.
         */
        public CallDiscardReason discardReason;
        /**
         * Call duration, in seconds.
         */
        public int duration;

        /**
         * Default constructor.
         */
        public MessageCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param discardReason Reason why the call was discarded.
         * @param duration Call duration, in seconds.
         */
        public MessageCall(CallDiscardReason discardReason, int duration) {
            this.discardReason = discardReason;
            this.duration = duration;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 366512596;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 366512596;
        }
    }

    /**
     * A newly created basic group.
     */
    public static class MessageBasicGroupChatCreate extends MessageContent {
        /**
         * Title of the basic group.
         */
        public String title;
        /**
         * User identifiers of members in the basic group.
         */
        public int[] memberUserIds;

        /**
         * Default constructor.
         */
        public MessageBasicGroupChatCreate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Title of the basic group.
         * @param memberUserIds User identifiers of members in the basic group.
         */
        public MessageBasicGroupChatCreate(String title, int[] memberUserIds) {
            this.title = title;
            this.memberUserIds = memberUserIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1575377646;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1575377646;
        }
    }

    /**
     * A newly created supergroup or channel.
     */
    public static class MessageSupergroupChatCreate extends MessageContent {
        /**
         * Title of the supergroup or channel.
         */
        public String title;

        /**
         * Default constructor.
         */
        public MessageSupergroupChatCreate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Title of the supergroup or channel.
         */
        public MessageSupergroupChatCreate(String title) {
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -434325733;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -434325733;
        }
    }

    /**
     * An updated chat title.
     */
    public static class MessageChatChangeTitle extends MessageContent {
        /**
         * New chat title.
         */
        public String title;

        /**
         * Default constructor.
         */
        public MessageChatChangeTitle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title New chat title.
         */
        public MessageChatChangeTitle(String title) {
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 748272449;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 748272449;
        }
    }

    /**
     * An updated chat photo.
     */
    public static class MessageChatChangePhoto extends MessageContent {
        /**
         * New chat photo.
         */
        public Photo photo;

        /**
         * Default constructor.
         */
        public MessageChatChangePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param photo New chat photo.
         */
        public MessageChatChangePhoto(Photo photo) {
            this.photo = photo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 319630249;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 319630249;
        }
    }

    /**
     * A deleted chat photo.
     */
    public static class MessageChatDeletePhoto extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageChatDeletePhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -184374809;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -184374809;
        }
    }

    /**
     * New chat members were added.
     */
    public static class MessageChatAddMembers extends MessageContent {
        /**
         * User identifiers of the new members.
         */
        public int[] memberUserIds;

        /**
         * Default constructor.
         */
        public MessageChatAddMembers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param memberUserIds User identifiers of the new members.
         */
        public MessageChatAddMembers(int[] memberUserIds) {
            this.memberUserIds = memberUserIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 401228326;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 401228326;
        }
    }

    /**
     * A new member joined the chat by invite link.
     */
    public static class MessageChatJoinByLink extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageChatJoinByLink() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1846493311;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1846493311;
        }
    }

    /**
     * A chat member was deleted.
     */
    public static class MessageChatDeleteMember extends MessageContent {
        /**
         * User identifier of the deleted chat member.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public MessageChatDeleteMember() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier of the deleted chat member.
         */
        public MessageChatDeleteMember(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1164414043;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1164414043;
        }
    }

    /**
     * A basic group was upgraded to a supergroup and was deactivated as the result.
     */
    public static class MessageChatUpgradeTo extends MessageContent {
        /**
         * Identifier of the supergroup to which the basic group was upgraded.
         */
        public int supergroupId;

        /**
         * Default constructor.
         */
        public MessageChatUpgradeTo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup to which the basic group was upgraded.
         */
        public MessageChatUpgradeTo(int supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1957816681;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1957816681;
        }
    }

    /**
     * A supergroup has been created from a basic group.
     */
    public static class MessageChatUpgradeFrom extends MessageContent {
        /**
         * Title of the newly created supergroup.
         */
        public String title;
        /**
         * The identifier of the original basic group.
         */
        public int basicGroupId;

        /**
         * Default constructor.
         */
        public MessageChatUpgradeFrom() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Title of the newly created supergroup.
         * @param basicGroupId The identifier of the original basic group.
         */
        public MessageChatUpgradeFrom(String title, int basicGroupId) {
            this.title = title;
            this.basicGroupId = basicGroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1642272558;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1642272558;
        }
    }

    /**
     * A message has been pinned.
     */
    public static class MessagePinMessage extends MessageContent {
        /**
         * Identifier of the pinned message, can be an identifier of a deleted message.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public MessagePinMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param messageId Identifier of the pinned message, can be an identifier of a deleted message.
         */
        public MessagePinMessage(long messageId) {
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 953503801;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 953503801;
        }
    }

    /**
     * A screenshot of a message in the chat has been taken.
     */
    public static class MessageScreenshotTaken extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageScreenshotTaken() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1564971605;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1564971605;
        }
    }

    /**
     * The TTL (Time To Live) setting messages in a secret chat has been changed.
     */
    public static class MessageChatSetTtl extends MessageContent {
        /**
         * New TTL.
         */
        public int ttl;

        /**
         * Default constructor.
         */
        public MessageChatSetTtl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param ttl New TTL.
         */
        public MessageChatSetTtl(int ttl) {
            this.ttl = ttl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1810060209;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1810060209;
        }
    }

    /**
     * A non-standard action has happened in the chat.
     */
    public static class MessageCustomServiceAction extends MessageContent {
        /**
         * Message text to be shown in the chat.
         */
        public String text;

        /**
         * Default constructor.
         */
        public MessageCustomServiceAction() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Message text to be shown in the chat.
         */
        public MessageCustomServiceAction(String text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1435879282;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1435879282;
        }
    }

    /**
     * A new high score was achieved in a game.
     */
    public static class MessageGameScore extends MessageContent {
        /**
         * Identifier of the message with the game, can be an identifier of a deleted message.
         */
        public long gameMessageId;
        /**
         * Identifier of the game, may be different from the games presented in the message with the game.
         */
        public long gameId;
        /**
         * New score.
         */
        public int score;

        /**
         * Default constructor.
         */
        public MessageGameScore() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param gameMessageId Identifier of the message with the game, can be an identifier of a deleted message.
         * @param gameId Identifier of the game, may be different from the games presented in the message with the game.
         * @param score New score.
         */
        public MessageGameScore(long gameMessageId, long gameId, int score) {
            this.gameMessageId = gameMessageId;
            this.gameId = gameId;
            this.score = score;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1344904575;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1344904575;
        }
    }

    /**
     * A payment has been completed.
     */
    public static class MessagePaymentSuccessful extends MessageContent {
        /**
         * Identifier of the message with the corresponding invoice; can be an identifier of a deleted message.
         */
        public long invoiceMessageId;
        /**
         * Currency for the price of the product.
         */
        public String currency;
        /**
         * Total price for the product, in the minimal quantity of the currency.
         */
        public long totalAmount;

        /**
         * Default constructor.
         */
        public MessagePaymentSuccessful() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param invoiceMessageId Identifier of the message with the corresponding invoice; can be an identifier of a deleted message.
         * @param currency Currency for the price of the product.
         * @param totalAmount Total price for the product, in the minimal quantity of the currency.
         */
        public MessagePaymentSuccessful(long invoiceMessageId, String currency, long totalAmount) {
            this.invoiceMessageId = invoiceMessageId;
            this.currency = currency;
            this.totalAmount = totalAmount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -595962993;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -595962993;
        }
    }

    /**
     * A payment has been completed; for bots only.
     */
    public static class MessagePaymentSuccessfulBot extends MessageContent {
        /**
         * Identifier of the message with the corresponding invoice; can be an identifier of a deleted message.
         */
        public long invoiceMessageId;
        /**
         * Currency for price of the product.
         */
        public String currency;
        /**
         * Total price for the product, in the minimal quantity of the currency.
         */
        public long totalAmount;
        /**
         * Invoice payload.
         */
        public byte[] invoicePayload;
        /**
         * Identifier of the shipping option chosen by the user, may be empty if not applicable.
         */
        public String shippingOptionId;
        /**
         * Information about the order; may be null.
         */
        public @Nullable OrderInfo orderInfo;
        /**
         * Telegram payment identifier.
         */
        public String telegramPaymentChargeId;
        /**
         * Provider payment identifier.
         */
        public String providerPaymentChargeId;

        /**
         * Default constructor.
         */
        public MessagePaymentSuccessfulBot() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param invoiceMessageId Identifier of the message with the corresponding invoice; can be an identifier of a deleted message.
         * @param currency Currency for price of the product.
         * @param totalAmount Total price for the product, in the minimal quantity of the currency.
         * @param invoicePayload Invoice payload.
         * @param shippingOptionId Identifier of the shipping option chosen by the user, may be empty if not applicable.
         * @param orderInfo Information about the order; may be null.
         * @param telegramPaymentChargeId Telegram payment identifier.
         * @param providerPaymentChargeId Provider payment identifier.
         */
        public MessagePaymentSuccessfulBot(long invoiceMessageId, String currency, long totalAmount, byte[] invoicePayload, String shippingOptionId, OrderInfo orderInfo, String telegramPaymentChargeId, String providerPaymentChargeId) {
            this.invoiceMessageId = invoiceMessageId;
            this.currency = currency;
            this.totalAmount = totalAmount;
            this.invoicePayload = invoicePayload;
            this.shippingOptionId = shippingOptionId;
            this.orderInfo = orderInfo;
            this.telegramPaymentChargeId = telegramPaymentChargeId;
            this.providerPaymentChargeId = providerPaymentChargeId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -412310696;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -412310696;
        }
    }

    /**
     * A contact has registered with Telegram.
     */
    public static class MessageContactRegistered extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageContactRegistered() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1502020353;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1502020353;
        }
    }

    /**
     * The current user has connected a website by logging in using Telegram Login Widget on it.
     */
    public static class MessageWebsiteConnected extends MessageContent {
        /**
         * Domain name of the connected website.
         */
        public String domainName;

        /**
         * Default constructor.
         */
        public MessageWebsiteConnected() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param domainName Domain name of the connected website.
         */
        public MessageWebsiteConnected(String domainName) {
            this.domainName = domainName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1074551800;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1074551800;
        }
    }

    /**
     * Message content that is not supported by the client.
     */
    public static class MessageUnsupported extends MessageContent {

        /**
         * Default constructor.
         */
        public MessageUnsupported() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1816726139;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1816726139;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains information about the initial sender of a forwarded message.
     */
    public abstract static class MessageForwardInfo extends Object {
    }

    /**
     * The message was originally written by a known user.
     */
    public static class MessageForwardedFromUser extends MessageForwardInfo {
        /**
         * Identifier of the user that originally sent this message.
         */
        public int senderUserId;
        /**
         * Point in time (Unix timestamp) when the message was originally sent.
         */
        public int date;
        /**
         * For messages forwarded to the chat with the current user (saved messages), the identifier of the chat from which the message was forwarded; 0 if unknown.
         */
        public long forwardedFromChatId;
        /**
         * For messages forwarded to the chat with the current user (saved messages) the identifier of the original message from which the new message was forwarded; 0 if unknown.
         */
        public long forwardedFromMessageId;

        /**
         * Default constructor.
         */
        public MessageForwardedFromUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param senderUserId Identifier of the user that originally sent this message.
         * @param date Point in time (Unix timestamp) when the message was originally sent.
         * @param forwardedFromChatId For messages forwarded to the chat with the current user (saved messages), the identifier of the chat from which the message was forwarded; 0 if unknown.
         * @param forwardedFromMessageId For messages forwarded to the chat with the current user (saved messages) the identifier of the original message from which the new message was forwarded; 0 if unknown.
         */
        public MessageForwardedFromUser(int senderUserId, int date, long forwardedFromChatId, long forwardedFromMessageId) {
            this.senderUserId = senderUserId;
            this.date = date;
            this.forwardedFromChatId = forwardedFromChatId;
            this.forwardedFromMessageId = forwardedFromMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1004332765;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1004332765;
        }
    }

    /**
     * The message was originally a post in a channel.
     */
    public static class MessageForwardedPost extends MessageForwardInfo {
        /**
         * Identifier of the chat from which the message was forwarded.
         */
        public long chatId;
        /**
         * Post author signature.
         */
        public String authorSignature;
        /**
         * Point in time (Unix timestamp) when the message was originally sent.
         */
        public int date;
        /**
         * Message identifier of the original message from which the new message was forwarded; 0 if unknown.
         */
        public long messageId;
        /**
         * For messages forwarded to the chat with the current user (saved messages), the identifier of the chat from which the message was forwarded; 0 if unknown.
         */
        public long forwardedFromChatId;
        /**
         * For messages forwarded to the chat with the current user (saved messages), the identifier of the original message from which the new message was forwarded; 0 if unknown.
         */
        public long forwardedFromMessageId;

        /**
         * Default constructor.
         */
        public MessageForwardedPost() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat from which the message was forwarded.
         * @param authorSignature Post author signature.
         * @param date Point in time (Unix timestamp) when the message was originally sent.
         * @param messageId Message identifier of the original message from which the new message was forwarded; 0 if unknown.
         * @param forwardedFromChatId For messages forwarded to the chat with the current user (saved messages), the identifier of the chat from which the message was forwarded; 0 if unknown.
         * @param forwardedFromMessageId For messages forwarded to the chat with the current user (saved messages), the identifier of the original message from which the new message was forwarded; 0 if unknown.
         */
        public MessageForwardedPost(long chatId, String authorSignature, int date, long messageId, long forwardedFromChatId, long forwardedFromMessageId) {
            this.chatId = chatId;
            this.authorSignature = authorSignature;
            this.date = date;
            this.messageId = messageId;
            this.forwardedFromChatId = forwardedFromChatId;
            this.forwardedFromMessageId = forwardedFromMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -244050875;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -244050875;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains information about the sending state of the message.
     */
    public abstract static class MessageSendingState extends Object {
    }

    /**
     * The message is being sent now, but has not yet been delivered to the server.
     */
    public static class MessageSendingStatePending extends MessageSendingState {

        /**
         * Default constructor.
         */
        public MessageSendingStatePending() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1381803582;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1381803582;
        }
    }

    /**
     * The message failed to be sent.
     */
    public static class MessageSendingStateFailed extends MessageSendingState {

        /**
         * Default constructor.
         */
        public MessageSendingStateFailed() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -546610323;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -546610323;
        }
    }

    /**
     * Contains a list of messages.
     */
    public static class Messages extends Object {
        /**
         * Approximate total count of messages found.
         */
        public int totalCount;
        /**
         * List of messages; messages may be null.
         */
        public Message[] messages;

        /**
         * Default constructor.
         */
        public Messages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param totalCount Approximate total count of messages found.
         * @param messages List of messages; messages may be null.
         */
        public Messages(int totalCount, Message[] messages) {
            this.totalCount = totalCount;
            this.messages = messages;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -16498159;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -16498159;
        }
    }

    /**
     * A full list of available network statistic entries.
     */
    public static class NetworkStatistics extends Object {
        /**
         * Point in time (Unix timestamp) when the app began collecting statistics.
         */
        public int sinceDate;
        /**
         * Network statistics entries.
         */
        public NetworkStatisticsEntry[] entries;

        /**
         * Default constructor.
         */
        public NetworkStatistics() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sinceDate Point in time (Unix timestamp) when the app began collecting statistics.
         * @param entries Network statistics entries.
         */
        public NetworkStatistics(int sinceDate, NetworkStatisticsEntry[] entries) {
            this.sinceDate = sinceDate;
            this.entries = entries;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1615554212;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1615554212;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains statistics about network usage.
     */
    public abstract static class NetworkStatisticsEntry extends Object {
    }

    /**
     * Contains information about the total amount of data that was used to send and receive files.
     */
    public static class NetworkStatisticsEntryFile extends NetworkStatisticsEntry {
        /**
         * Type of the file the data is part of.
         */
        public FileType fileType;
        /**
         * Type of the network the data was sent through. Call setNetworkType to maintain the actual network type.
         */
        public NetworkType networkType;
        /**
         * Total number of bytes sent.
         */
        public long sentBytes;
        /**
         * Total number of bytes received.
         */
        public long receivedBytes;

        /**
         * Default constructor.
         */
        public NetworkStatisticsEntryFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileType Type of the file the data is part of.
         * @param networkType Type of the network the data was sent through. Call setNetworkType to maintain the actual network type.
         * @param sentBytes Total number of bytes sent.
         * @param receivedBytes Total number of bytes received.
         */
        public NetworkStatisticsEntryFile(FileType fileType, NetworkType networkType, long sentBytes, long receivedBytes) {
            this.fileType = fileType;
            this.networkType = networkType;
            this.sentBytes = sentBytes;
            this.receivedBytes = receivedBytes;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 188452706;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 188452706;
        }
    }

    /**
     * Contains information about the total amount of data that was used for calls.
     */
    public static class NetworkStatisticsEntryCall extends NetworkStatisticsEntry {
        /**
         * Type of the network the data was sent through. Call setNetworkType to maintain the actual network type.
         */
        public NetworkType networkType;
        /**
         * Total number of bytes sent.
         */
        public long sentBytes;
        /**
         * Total number of bytes received.
         */
        public long receivedBytes;
        /**
         * Total call duration, in seconds.
         */
        public double duration;

        /**
         * Default constructor.
         */
        public NetworkStatisticsEntryCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param networkType Type of the network the data was sent through. Call setNetworkType to maintain the actual network type.
         * @param sentBytes Total number of bytes sent.
         * @param receivedBytes Total number of bytes received.
         * @param duration Total call duration, in seconds.
         */
        public NetworkStatisticsEntryCall(NetworkType networkType, long sentBytes, long receivedBytes, double duration) {
            this.networkType = networkType;
            this.sentBytes = sentBytes;
            this.receivedBytes = receivedBytes;
            this.duration = duration;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 737000365;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 737000365;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the type of a network.
     */
    public abstract static class NetworkType extends Object {
    }

    /**
     * The network is not available.
     */
    public static class NetworkTypeNone extends NetworkType {

        /**
         * Default constructor.
         */
        public NetworkTypeNone() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1971691759;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1971691759;
        }
    }

    /**
     * A mobile network.
     */
    public static class NetworkTypeMobile extends NetworkType {

        /**
         * Default constructor.
         */
        public NetworkTypeMobile() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 819228239;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 819228239;
        }
    }

    /**
     * A mobile roaming network.
     */
    public static class NetworkTypeMobileRoaming extends NetworkType {

        /**
         * Default constructor.
         */
        public NetworkTypeMobileRoaming() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1435199760;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1435199760;
        }
    }

    /**
     * A Wi-Fi network.
     */
    public static class NetworkTypeWiFi extends NetworkType {

        /**
         * Default constructor.
         */
        public NetworkTypeWiFi() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -633872070;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -633872070;
        }
    }

    /**
     * A different network type (e.g., Ethernet network).
     */
    public static class NetworkTypeOther extends NetworkType {

        /**
         * Default constructor.
         */
        public NetworkTypeOther() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1942128539;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1942128539;
        }
    }

    /**
     * Contains information about notification settings for a chat or several chats.
     */
    public static class NotificationSettings extends Object {
        /**
         * Time left before notifications will be unmuted, in seconds.
         */
        public int muteFor;
        /**
         * An audio file name for notification sounds; only applies to iOS applications.
         */
        public String sound;
        /**
         * True, if message content should be displayed in notifications.
         */
        public boolean showPreview;

        /**
         * Default constructor.
         */
        public NotificationSettings() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param muteFor Time left before notifications will be unmuted, in seconds.
         * @param sound An audio file name for notification sounds; only applies to iOS applications.
         * @param showPreview True, if message content should be displayed in notifications.
         */
        public NotificationSettings(int muteFor, String sound, boolean showPreview) {
            this.muteFor = muteFor;
            this.sound = sound;
            this.showPreview = showPreview;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1737538681;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1737538681;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the types of chats for which notification settings are applied.
     */
    public abstract static class NotificationSettingsScope extends Object {
    }

    /**
     * Notification settings applied to a particular chat.
     */
    public static class NotificationSettingsScopeChat extends NotificationSettingsScope {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public NotificationSettingsScopeChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public NotificationSettingsScopeChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1855845499;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1855845499;
        }
    }

    /**
     * Notification settings applied to all private chats.
     */
    public static class NotificationSettingsScopePrivateChats extends NotificationSettingsScope {

        /**
         * Default constructor.
         */
        public NotificationSettingsScopePrivateChats() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 937446759;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 937446759;
        }
    }

    /**
     * Notification settings applied to all basic groups and channels. (Supergroups have no common settings.)
     */
    public static class NotificationSettingsScopeBasicGroupChats extends NotificationSettingsScope {

        /**
         * Default constructor.
         */
        public NotificationSettingsScopeBasicGroupChats() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1358646601;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1358646601;
        }
    }

    /**
     * Notification settings applied to all chats.
     */
    public static class NotificationSettingsScopeAllChats extends NotificationSettingsScope {

        /**
         * Default constructor.
         */
        public NotificationSettingsScopeAllChats() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1345889922;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1345889922;
        }
    }

    /**
     * An object of this type is returned on a successful function call for certain functions.
     */
    public static class Ok extends Object {

        /**
         * Default constructor.
         */
        public Ok() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -722616727;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -722616727;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the value of an option.
     */
    public abstract static class OptionValue extends Object {
    }

    /**
     * Boolean option.
     */
    public static class OptionValueBoolean extends OptionValue {
        /**
         * The value of the option.
         */
        public boolean value;

        /**
         * Default constructor.
         */
        public OptionValueBoolean() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value The value of the option.
         */
        public OptionValueBoolean(boolean value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 63135518;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 63135518;
        }
    }

    /**
     * An unknown option or an option which has a default value.
     */
    public static class OptionValueEmpty extends OptionValue {

        /**
         * Default constructor.
         */
        public OptionValueEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 918955155;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 918955155;
        }
    }

    /**
     * An integer option.
     */
    public static class OptionValueInteger extends OptionValue {
        /**
         * The value of the option.
         */
        public int value;

        /**
         * Default constructor.
         */
        public OptionValueInteger() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value The value of the option.
         */
        public OptionValueInteger(int value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1400911104;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1400911104;
        }
    }

    /**
     * A string option.
     */
    public static class OptionValueString extends OptionValue {
        /**
         * The value of the option.
         */
        public String value;

        /**
         * Default constructor.
         */
        public OptionValueString() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value The value of the option.
         */
        public OptionValueString(String value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 756248212;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 756248212;
        }
    }

    /**
     * Order information.
     */
    public static class OrderInfo extends Object {
        /**
         * Name of the user.
         */
        public String name;
        /**
         * Phone number of the user.
         */
        public String phoneNumber;
        /**
         * Email address of the user.
         */
        public String emailAddress;
        /**
         * Shipping address for this order; may be null.
         */
        public @Nullable ShippingAddress shippingAddress;

        /**
         * Default constructor.
         */
        public OrderInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name Name of the user.
         * @param phoneNumber Phone number of the user.
         * @param emailAddress Email address of the user.
         * @param shippingAddress Shipping address for this order; may be null.
         */
        public OrderInfo(String name, String phoneNumber, String emailAddress, ShippingAddress shippingAddress) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.emailAddress = emailAddress;
            this.shippingAddress = shippingAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1830611784;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1830611784;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes a block of an instant view web page.
     */
    public abstract static class PageBlock extends Object {
    }

    /**
     * The title of a page.
     */
    public static class PageBlockTitle extends PageBlock {
        /**
         * Title.
         */
        public RichText title;

        /**
         * Default constructor.
         */
        public PageBlockTitle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Title.
         */
        public PageBlockTitle(RichText title) {
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1629664784;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1629664784;
        }
    }

    /**
     * The subtitle of a page.
     */
    public static class PageBlockSubtitle extends PageBlock {
        /**
         * Subtitle.
         */
        public RichText subtitle;

        /**
         * Default constructor.
         */
        public PageBlockSubtitle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param subtitle Subtitle.
         */
        public PageBlockSubtitle(RichText subtitle) {
            this.subtitle = subtitle;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 264524263;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 264524263;
        }
    }

    /**
     * The author and publishing date of a page.
     */
    public static class PageBlockAuthorDate extends PageBlock {
        /**
         * Author.
         */
        public RichText author;
        /**
         * Point in time (Unix timestamp) when the article was published; 0 if unknown.
         */
        public int publishDate;

        /**
         * Default constructor.
         */
        public PageBlockAuthorDate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param author Author.
         * @param publishDate Point in time (Unix timestamp) when the article was published; 0 if unknown.
         */
        public PageBlockAuthorDate(RichText author, int publishDate) {
            this.author = author;
            this.publishDate = publishDate;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1300231184;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1300231184;
        }
    }

    /**
     * A header.
     */
    public static class PageBlockHeader extends PageBlock {
        /**
         * Header.
         */
        public RichText header;

        /**
         * Default constructor.
         */
        public PageBlockHeader() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param header Header.
         */
        public PageBlockHeader(RichText header) {
            this.header = header;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1402854811;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1402854811;
        }
    }

    /**
     * A subheader.
     */
    public static class PageBlockSubheader extends PageBlock {
        /**
         * Subheader.
         */
        public RichText subheader;

        /**
         * Default constructor.
         */
        public PageBlockSubheader() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param subheader Subheader.
         */
        public PageBlockSubheader(RichText subheader) {
            this.subheader = subheader;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1263956774;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1263956774;
        }
    }

    /**
     * A text paragraph.
     */
    public static class PageBlockParagraph extends PageBlock {
        /**
         * Paragraph text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public PageBlockParagraph() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Paragraph text.
         */
        public PageBlockParagraph(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1182402406;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1182402406;
        }
    }

    /**
     * A preformatted text paragraph.
     */
    public static class PageBlockPreformatted extends PageBlock {
        /**
         * Paragraph text.
         */
        public RichText text;
        /**
         * Programming language for which the text should be formatted.
         */
        public String language;

        /**
         * Default constructor.
         */
        public PageBlockPreformatted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Paragraph text.
         * @param language Programming language for which the text should be formatted.
         */
        public PageBlockPreformatted(RichText text, String language) {
            this.text = text;
            this.language = language;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1066346178;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1066346178;
        }
    }

    /**
     * The footer of a page.
     */
    public static class PageBlockFooter extends PageBlock {
        /**
         * Footer.
         */
        public RichText footer;

        /**
         * Default constructor.
         */
        public PageBlockFooter() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param footer Footer.
         */
        public PageBlockFooter(RichText footer) {
            this.footer = footer;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 886429480;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 886429480;
        }
    }

    /**
     * An empty block separating a page.
     */
    public static class PageBlockDivider extends PageBlock {

        /**
         * Default constructor.
         */
        public PageBlockDivider() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -618614392;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -618614392;
        }
    }

    /**
     * An invisible anchor on a page, which can be used in a URL to open the page from the specified anchor.
     */
    public static class PageBlockAnchor extends PageBlock {
        /**
         * Name of the anchor.
         */
        public String name;

        /**
         * Default constructor.
         */
        public PageBlockAnchor() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name Name of the anchor.
         */
        public PageBlockAnchor(String name) {
            this.name = name;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -837994576;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -837994576;
        }
    }

    /**
     * A list of texts.
     */
    public static class PageBlockList extends PageBlock {
        /**
         * Texts.
         */
        public RichText[] items;
        /**
         * True, if the items should be marked with numbers.
         */
        public boolean isOrdered;

        /**
         * Default constructor.
         */
        public PageBlockList() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param items Texts.
         * @param isOrdered True, if the items should be marked with numbers.
         */
        public PageBlockList(RichText[] items, boolean isOrdered) {
            this.items = items;
            this.isOrdered = isOrdered;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1752631674;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1752631674;
        }
    }

    /**
     * A block quote.
     */
    public static class PageBlockBlockQuote extends PageBlock {
        /**
         * Quote text.
         */
        public RichText text;
        /**
         * Quote caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockBlockQuote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Quote text.
         * @param caption Quote caption.
         */
        public PageBlockBlockQuote(RichText text, RichText caption) {
            this.text = text;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -37421406;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -37421406;
        }
    }

    /**
     * A pull quote.
     */
    public static class PageBlockPullQuote extends PageBlock {
        /**
         * Quote text.
         */
        public RichText text;
        /**
         * Quote caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockPullQuote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Quote text.
         * @param caption Quote caption.
         */
        public PageBlockPullQuote(RichText text, RichText caption) {
            this.text = text;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1799498665;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1799498665;
        }
    }

    /**
     * An animation.
     */
    public static class PageBlockAnimation extends PageBlock {
        /**
         * Animation file; may be null.
         */
        public @Nullable Animation animation;
        /**
         * Animation caption.
         */
        public RichText caption;
        /**
         * True, if the animation should be played automatically.
         */
        public boolean needAutoplay;

        /**
         * Default constructor.
         */
        public PageBlockAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animation Animation file; may be null.
         * @param caption Animation caption.
         * @param needAutoplay True, if the animation should be played automatically.
         */
        public PageBlockAnimation(Animation animation, RichText caption, boolean needAutoplay) {
            this.animation = animation;
            this.caption = caption;
            this.needAutoplay = needAutoplay;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1639478661;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1639478661;
        }
    }

    /**
     * An audio file.
     */
    public static class PageBlockAudio extends PageBlock {
        /**
         * Audio file; may be null.
         */
        public @Nullable Audio audio;
        /**
         * Audio file caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockAudio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param audio Audio file; may be null.
         * @param caption Audio file caption.
         */
        public PageBlockAudio(Audio audio, RichText caption) {
            this.audio = audio;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1898245855;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1898245855;
        }
    }

    /**
     * A photo.
     */
    public static class PageBlockPhoto extends PageBlock {
        /**
         * Photo file; may be null.
         */
        public @Nullable Photo photo;
        /**
         * Photo caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param photo Photo file; may be null.
         * @param caption Photo caption.
         */
        public PageBlockPhoto(Photo photo, RichText caption) {
            this.photo = photo;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -637181825;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -637181825;
        }
    }

    /**
     * A video.
     */
    public static class PageBlockVideo extends PageBlock {
        /**
         * Video file; may be null.
         */
        public @Nullable Video video;
        /**
         * Video caption.
         */
        public RichText caption;
        /**
         * True, if the video should be played automatically.
         */
        public boolean needAutoplay;
        /**
         * True, if the video should be looped.
         */
        public boolean isLooped;

        /**
         * Default constructor.
         */
        public PageBlockVideo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param video Video file; may be null.
         * @param caption Video caption.
         * @param needAutoplay True, if the video should be played automatically.
         * @param isLooped True, if the video should be looped.
         */
        public PageBlockVideo(Video video, RichText caption, boolean needAutoplay, boolean isLooped) {
            this.video = video;
            this.caption = caption;
            this.needAutoplay = needAutoplay;
            this.isLooped = isLooped;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1610373002;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1610373002;
        }
    }

    /**
     * A page cover.
     */
    public static class PageBlockCover extends PageBlock {
        /**
         * Cover.
         */
        public PageBlock cover;

        /**
         * Default constructor.
         */
        public PageBlockCover() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param cover Cover.
         */
        public PageBlockCover(PageBlock cover) {
            this.cover = cover;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 972174080;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 972174080;
        }
    }

    /**
     * An embedded web page.
     */
    public static class PageBlockEmbedded extends PageBlock {
        /**
         * Web page URL, if available.
         */
        public String url;
        /**
         * HTML-markup of the embedded page.
         */
        public String html;
        /**
         * Poster photo, if available; may be null.
         */
        public @Nullable Photo posterPhoto;
        /**
         * Block width.
         */
        public int width;
        /**
         * Block height.
         */
        public int height;
        /**
         * Block caption.
         */
        public RichText caption;
        /**
         * True, if the block should be full width.
         */
        public boolean isFullWidth;
        /**
         * True, if scrolling should be allowed.
         */
        public boolean allowScrolling;

        /**
         * Default constructor.
         */
        public PageBlockEmbedded() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url Web page URL, if available.
         * @param html HTML-markup of the embedded page.
         * @param posterPhoto Poster photo, if available; may be null.
         * @param width Block width.
         * @param height Block height.
         * @param caption Block caption.
         * @param isFullWidth True, if the block should be full width.
         * @param allowScrolling True, if scrolling should be allowed.
         */
        public PageBlockEmbedded(String url, String html, Photo posterPhoto, int width, int height, RichText caption, boolean isFullWidth, boolean allowScrolling) {
            this.url = url;
            this.html = html;
            this.posterPhoto = posterPhoto;
            this.width = width;
            this.height = height;
            this.caption = caption;
            this.isFullWidth = isFullWidth;
            this.allowScrolling = allowScrolling;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -211257334;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -211257334;
        }
    }

    /**
     * An embedded post.
     */
    public static class PageBlockEmbeddedPost extends PageBlock {
        /**
         * Web page URL.
         */
        public String url;
        /**
         * Post author.
         */
        public String author;
        /**
         * Post author photo.
         */
        public Photo authorPhoto;
        /**
         * Point in time (Unix timestamp) when the post was created; 0 if unknown.
         */
        public int date;
        /**
         * Post content.
         */
        public PageBlock[] pageBlocks;
        /**
         * Post caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockEmbeddedPost() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url Web page URL.
         * @param author Post author.
         * @param authorPhoto Post author photo.
         * @param date Point in time (Unix timestamp) when the post was created; 0 if unknown.
         * @param pageBlocks Post content.
         * @param caption Post caption.
         */
        public PageBlockEmbeddedPost(String url, String author, Photo authorPhoto, int date, PageBlock[] pageBlocks, RichText caption) {
            this.url = url;
            this.author = author;
            this.authorPhoto = authorPhoto;
            this.date = date;
            this.pageBlocks = pageBlocks;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1049948772;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1049948772;
        }
    }

    /**
     * A collage.
     */
    public static class PageBlockCollage extends PageBlock {
        /**
         * Collage item contents.
         */
        public PageBlock[] pageBlocks;
        /**
         * Block caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockCollage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pageBlocks Collage item contents.
         * @param caption Block caption.
         */
        public PageBlockCollage(PageBlock[] pageBlocks, RichText caption) {
            this.pageBlocks = pageBlocks;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 911142202;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 911142202;
        }
    }

    /**
     * A slideshow.
     */
    public static class PageBlockSlideshow extends PageBlock {
        /**
         * Slideshow item contents.
         */
        public PageBlock[] pageBlocks;
        /**
         * Block caption.
         */
        public RichText caption;

        /**
         * Default constructor.
         */
        public PageBlockSlideshow() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pageBlocks Slideshow item contents.
         * @param caption Block caption.
         */
        public PageBlockSlideshow(PageBlock[] pageBlocks, RichText caption) {
            this.pageBlocks = pageBlocks;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 178557514;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 178557514;
        }
    }

    /**
     * A link to a chat.
     */
    public static class PageBlockChatLink extends PageBlock {
        /**
         * Chat title.
         */
        public String title;
        /**
         * Chat photo; may be null.
         */
        public @Nullable ChatPhoto photo;
        /**
         * Chat username, by which all other information about the chat should be resolved.
         */
        public String username;

        /**
         * Default constructor.
         */
        public PageBlockChatLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Chat title.
         * @param photo Chat photo; may be null.
         * @param username Chat username, by which all other information about the chat should be resolved.
         */
        public PageBlockChatLink(String title, ChatPhoto photo, String username) {
            this.title = title;
            this.photo = photo;
            this.username = username;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 214606645;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 214606645;
        }
    }

    /**
     * Contains information available to the user after requesting password recovery.
     */
    public static class PasswordRecoveryInfo extends Object {
        /**
         * Pattern of the email address to which a recovery email was sent.
         */
        public String recoveryEmailAddressPattern;

        /**
         * Default constructor.
         */
        public PasswordRecoveryInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param recoveryEmailAddressPattern Pattern of the email address to which a recovery email was sent.
         */
        public PasswordRecoveryInfo(String recoveryEmailAddressPattern) {
            this.recoveryEmailAddressPattern = recoveryEmailAddressPattern;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1483233330;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1483233330;
        }
    }

    /**
     * Represents the current state of 2-step verification.
     */
    public static class PasswordState extends Object {
        /**
         * True if a 2-step verification password has been set up.
         */
        public boolean hasPassword;
        /**
         * Hint for the password; can be empty.
         */
        public String passwordHint;
        /**
         * True if a recovery email has been set up.
         */
        public boolean hasRecoveryEmailAddress;
        /**
         * Pattern of the email address to which a confirmation email was sent.
         */
        public String unconfirmedRecoveryEmailAddressPattern;

        /**
         * Default constructor.
         */
        public PasswordState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param hasPassword True if a 2-step verification password has been set up.
         * @param passwordHint Hint for the password; can be empty.
         * @param hasRecoveryEmailAddress True if a recovery email has been set up.
         * @param unconfirmedRecoveryEmailAddressPattern Pattern of the email address to which a confirmation email was sent.
         */
        public PasswordState(boolean hasPassword, String passwordHint, boolean hasRecoveryEmailAddress, String unconfirmedRecoveryEmailAddressPattern) {
            this.hasPassword = hasPassword;
            this.passwordHint = passwordHint;
            this.hasRecoveryEmailAddress = hasRecoveryEmailAddress;
            this.unconfirmedRecoveryEmailAddressPattern = unconfirmedRecoveryEmailAddressPattern;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1383061922;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1383061922;
        }
    }

    /**
     * Contains information about an invoice payment form.
     */
    public static class PaymentForm extends Object {
        /**
         * Full information of the invoice.
         */
        public Invoice invoice;
        /**
         * Payment form URL.
         */
        public String url;
        /**
         * Contains information about the payment provider, if available, to support it natively without the need for opening the URL; may be null.
         */
        public @Nullable PaymentsProviderStripe paymentsProvider;
        /**
         * Saved server-side order information; may be null.
         */
        public @Nullable OrderInfo savedOrderInfo;
        /**
         * Contains information about saved card credentials; may be null.
         */
        public @Nullable SavedCredentials savedCredentials;
        /**
         * True, if the user can choose to save credentials.
         */
        public boolean canSaveCredentials;
        /**
         * True, if the user will be able to save credentials protected by a password they set up.
         */
        public boolean needPassword;

        /**
         * Default constructor.
         */
        public PaymentForm() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param invoice Full information of the invoice.
         * @param url Payment form URL.
         * @param paymentsProvider Contains information about the payment provider, if available, to support it natively without the need for opening the URL; may be null.
         * @param savedOrderInfo Saved server-side order information; may be null.
         * @param savedCredentials Contains information about saved card credentials; may be null.
         * @param canSaveCredentials True, if the user can choose to save credentials.
         * @param needPassword True, if the user will be able to save credentials protected by a password they set up.
         */
        public PaymentForm(Invoice invoice, String url, PaymentsProviderStripe paymentsProvider, OrderInfo savedOrderInfo, SavedCredentials savedCredentials, boolean canSaveCredentials, boolean needPassword) {
            this.invoice = invoice;
            this.url = url;
            this.paymentsProvider = paymentsProvider;
            this.savedOrderInfo = savedOrderInfo;
            this.savedCredentials = savedCredentials;
            this.canSaveCredentials = canSaveCredentials;
            this.needPassword = needPassword;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -200418230;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -200418230;
        }
    }

    /**
     * Contains information about a successful payment.
     */
    public static class PaymentReceipt extends Object {
        /**
         * Point in time (Unix timestamp) when the payment was made.
         */
        public int date;
        /**
         * User identifier of the payment provider bot.
         */
        public int paymentsProviderUserId;
        /**
         * Contains information about the invoice.
         */
        public Invoice invoice;
        /**
         * Contains order information; may be null.
         */
        public @Nullable OrderInfo orderInfo;
        /**
         * Chosen shipping option; may be null.
         */
        public @Nullable ShippingOption shippingOption;
        /**
         * Title of the saved credentials.
         */
        public String credentialsTitle;

        /**
         * Default constructor.
         */
        public PaymentReceipt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param date Point in time (Unix timestamp) when the payment was made.
         * @param paymentsProviderUserId User identifier of the payment provider bot.
         * @param invoice Contains information about the invoice.
         * @param orderInfo Contains order information; may be null.
         * @param shippingOption Chosen shipping option; may be null.
         * @param credentialsTitle Title of the saved credentials.
         */
        public PaymentReceipt(int date, int paymentsProviderUserId, Invoice invoice, OrderInfo orderInfo, ShippingOption shippingOption, String credentialsTitle) {
            this.date = date;
            this.paymentsProviderUserId = paymentsProviderUserId;
            this.invoice = invoice;
            this.orderInfo = orderInfo;
            this.shippingOption = shippingOption;
            this.credentialsTitle = credentialsTitle;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1171223545;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1171223545;
        }
    }

    /**
     * Contains the result of a payment request.
     */
    public static class PaymentResult extends Object {
        /**
         * True, if the payment request was successful; otherwise the verificationUrl will be not empty.
         */
        public boolean success;
        /**
         * URL for additional payment credentials verification.
         */
        public String verificationUrl;

        /**
         * Default constructor.
         */
        public PaymentResult() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param success True, if the payment request was successful; otherwise the verificationUrl will be not empty.
         * @param verificationUrl URL for additional payment credentials verification.
         */
        public PaymentResult(boolean success, String verificationUrl) {
            this.success = success;
            this.verificationUrl = verificationUrl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -804263843;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -804263843;
        }
    }

    /**
     * Stripe payment provider.
     */
    public static class PaymentsProviderStripe extends Object {
        /**
         * Stripe API publishable key.
         */
        public String publishableKey;
        /**
         * True, if the user country must be provided.
         */
        public boolean needCountry;
        /**
         * True, if the user ZIP/postal code must be provided.
         */
        public boolean needPostalCode;
        /**
         * True, if the cardholder name must be provided.
         */
        public boolean needCardholderName;

        /**
         * Default constructor.
         */
        public PaymentsProviderStripe() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param publishableKey Stripe API publishable key.
         * @param needCountry True, if the user country must be provided.
         * @param needPostalCode True, if the user ZIP/postal code must be provided.
         * @param needCardholderName True, if the cardholder name must be provided.
         */
        public PaymentsProviderStripe(String publishableKey, boolean needCountry, boolean needPostalCode, boolean needCardholderName) {
            this.publishableKey = publishableKey;
            this.needCountry = needCountry;
            this.needPostalCode = needPostalCode;
            this.needCardholderName = needCardholderName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1090791032;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1090791032;
        }
    }

    /**
     * Describes a photo.
     */
    public static class Photo extends Object {
        /**
         * Photo identifier; 0 for deleted photos.
         */
        public long id;
        /**
         * True, if stickers were added to the photo.
         */
        public boolean hasStickers;
        /**
         * Available variants of the photo, in different sizes.
         */
        public PhotoSize[] sizes;

        /**
         * Default constructor.
         */
        public Photo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Photo identifier; 0 for deleted photos.
         * @param hasStickers True, if stickers were added to the photo.
         * @param sizes Available variants of the photo, in different sizes.
         */
        public Photo(long id, boolean hasStickers, PhotoSize[] sizes) {
            this.id = id;
            this.hasStickers = hasStickers;
            this.sizes = sizes;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1949521787;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1949521787;
        }
    }

    /**
     * Photo description.
     */
    public static class PhotoSize extends Object {
        /**
         * Thumbnail type (see https://core.telegram.org/constructor/photoSize).
         */
        public String type;
        /**
         * Information about the photo file.
         */
        public File photo;
        /**
         * Photo width.
         */
        public int width;
        /**
         * Photo height.
         */
        public int height;

        /**
         * Default constructor.
         */
        public PhotoSize() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param type Thumbnail type (see https://core.telegram.org/constructor/photoSize).
         * @param photo Information about the photo file.
         * @param width Photo width.
         * @param height Photo height.
         */
        public PhotoSize(String type, File photo, int width, int height) {
            this.type = type;
            this.photo = photo;
            this.width = width;
            this.height = height;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 421980227;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 421980227;
        }
    }

    /**
     * Describes a user profile photo.
     */
    public static class ProfilePhoto extends Object {
        /**
         * Photo identifier; 0 for an empty photo. Can be used to find a photo in a list of userProfilePhotos.
         */
        public long id;
        /**
         * A small (160x160) user profile photo.
         */
        public File small;
        /**
         * A big (640x640) user profile photo.
         */
        public File big;

        /**
         * Default constructor.
         */
        public ProfilePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Photo identifier; 0 for an empty photo. Can be used to find a photo in a list of userProfilePhotos.
         * @param small A small (160x160) user profile photo.
         * @param big A big (640x640) user profile photo.
         */
        public ProfilePhoto(long id, File small, File big) {
            this.id = id;
            this.small = small;
            this.big = big;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 978085937;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 978085937;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains information about a proxy server.
     */
    public abstract static class Proxy extends Object {
    }

    /**
     * An empty proxy server.
     */
    public static class ProxyEmpty extends Proxy {

        /**
         * Default constructor.
         */
        public ProxyEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 748440246;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 748440246;
        }
    }

    /**
     * A SOCKS5 proxy server.
     */
    public static class ProxySocks5 extends Proxy {
        /**
         * Proxy server IP address.
         */
        public String server;
        /**
         * Proxy server port.
         */
        public int port;
        /**
         * Username for logging in.
         */
        public String username;
        /**
         * Password for logging in.
         */
        public String password;

        /**
         * Default constructor.
         */
        public ProxySocks5() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param server Proxy server IP address.
         * @param port Proxy server port.
         * @param username Username for logging in.
         * @param password Password for logging in.
         */
        public ProxySocks5(String server, int port, String username, String password) {
            this.server = server;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1456461592;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1456461592;
        }
    }

    /**
     * Contains a public HTTPS link to a message in a public supergroup or channel.
     */
    public static class PublicMessageLink extends Object {
        /**
         * Message link.
         */
        public String link;
        /**
         * HTML-code for embedding the message.
         */
        public String html;

        /**
         * Default constructor.
         */
        public PublicMessageLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param link Message link.
         * @param html HTML-code for embedding the message.
         */
        public PublicMessageLink(String link, String html) {
            this.link = link;
            this.html = html;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -679603433;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -679603433;
        }
    }

    /**
     * Contains information about the current recovery email address.
     */
    public static class RecoveryEmailAddress extends Object {
        /**
         * Recovery email address.
         */
        public String recoveryEmailAddress;

        /**
         * Default constructor.
         */
        public RecoveryEmailAddress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param recoveryEmailAddress Recovery email address.
         */
        public RecoveryEmailAddress(String recoveryEmailAddress) {
            this.recoveryEmailAddress = recoveryEmailAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1290526187;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1290526187;
        }
    }

    /**
     * Represents a remote file.
     */
    public static class RemoteFile extends Object {
        /**
         * Remote file identifier, may be empty. Can be used across application restarts or even from other devices for the current user. If the ID starts with &quot;http://&quot; or &quot;https://&quot;, it represents the HTTP URL of the file. TDLib is currently unable to download files if only their URL is known. If downloadFile is called on such a file or if it is sent to a secret chat, TDLib starts a file generation process by sending updateFileGenerationStart to the client with the HTTP URL in the originalPath and &quot;#url#&quot; as the conversion string. Clients should generate the file by downloading it to the specified location.
         */
        public String id;
        /**
         * True, if the file is currently being uploaded (or a remote copy is being generated by some other means).
         */
        public boolean isUploadingActive;
        /**
         * True, if a remote copy is fully available.
         */
        public boolean isUploadingCompleted;
        /**
         * Size of the remote available part of the file; 0 if unknown.
         */
        public int uploadedSize;

        /**
         * Default constructor.
         */
        public RemoteFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Remote file identifier, may be empty. Can be used across application restarts or even from other devices for the current user. If the ID starts with &quot;http://&quot; or &quot;https://&quot;, it represents the HTTP URL of the file. TDLib is currently unable to download files if only their URL is known. If downloadFile is called on such a file or if it is sent to a secret chat, TDLib starts a file generation process by sending updateFileGenerationStart to the client with the HTTP URL in the originalPath and &quot;#url#&quot; as the conversion string. Clients should generate the file by downloading it to the specified location.
         * @param isUploadingActive True, if the file is currently being uploaded (or a remote copy is being generated by some other means).
         * @param isUploadingCompleted True, if a remote copy is fully available.
         * @param uploadedSize Size of the remote available part of the file; 0 if unknown.
         */
        public RemoteFile(String id, boolean isUploadingActive, boolean isUploadingCompleted, int uploadedSize) {
            this.id = id;
            this.isUploadingActive = isUploadingActive;
            this.isUploadingCompleted = isUploadingCompleted;
            this.uploadedSize = uploadedSize;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1761289748;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1761289748;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains a description of a custom keyboard and actions that can be done with it to quickly reply to bots.
     */
    public abstract static class ReplyMarkup extends Object {
    }

    /**
     * Instructs clients to remove the keyboard once this message has been received. This kind of keyboard can't be received in an incoming message; instead, UpdateChatReplyMarkup with messageId == 0 will be sent.
     */
    public static class ReplyMarkupRemoveKeyboard extends ReplyMarkup {
        /**
         * True, if the keyboard is removed only for the mentioned users or the target user of a reply.
         */
        public boolean isPersonal;

        /**
         * Default constructor.
         */
        public ReplyMarkupRemoveKeyboard() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isPersonal True, if the keyboard is removed only for the mentioned users or the target user of a reply.
         */
        public ReplyMarkupRemoveKeyboard(boolean isPersonal) {
            this.isPersonal = isPersonal;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -691252879;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -691252879;
        }
    }

    /**
     * Instructs clients to force a reply to this message.
     */
    public static class ReplyMarkupForceReply extends ReplyMarkup {
        /**
         * True, if a forced reply must automatically be shown to the current user. For outgoing messages, specify true to show the forced reply only for the mentioned users and for the target user of a reply.
         */
        public boolean isPersonal;

        /**
         * Default constructor.
         */
        public ReplyMarkupForceReply() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isPersonal True, if a forced reply must automatically be shown to the current user. For outgoing messages, specify true to show the forced reply only for the mentioned users and for the target user of a reply.
         */
        public ReplyMarkupForceReply(boolean isPersonal) {
            this.isPersonal = isPersonal;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1039104593;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1039104593;
        }
    }

    /**
     * Contains a custom keyboard layout to quickly reply to bots.
     */
    public static class ReplyMarkupShowKeyboard extends ReplyMarkup {
        /**
         * A list of rows of bot keyboard buttons.
         */
        public KeyboardButton[][] rows;
        /**
         * True, if the client needs to resize the keyboard vertically.
         */
        public boolean resizeKeyboard;
        /**
         * True, if the client needs to hide the keyboard after use.
         */
        public boolean oneTime;
        /**
         * True, if the keyboard must automatically be shown to the current user. For outgoing messages, specify true to show the keyboard only for the mentioned users and for the target user of a reply.
         */
        public boolean isPersonal;

        /**
         * Default constructor.
         */
        public ReplyMarkupShowKeyboard() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param rows A list of rows of bot keyboard buttons.
         * @param resizeKeyboard True, if the client needs to resize the keyboard vertically.
         * @param oneTime True, if the client needs to hide the keyboard after use.
         * @param isPersonal True, if the keyboard must automatically be shown to the current user. For outgoing messages, specify true to show the keyboard only for the mentioned users and for the target user of a reply.
         */
        public ReplyMarkupShowKeyboard(KeyboardButton[][] rows, boolean resizeKeyboard, boolean oneTime, boolean isPersonal) {
            this.rows = rows;
            this.resizeKeyboard = resizeKeyboard;
            this.oneTime = oneTime;
            this.isPersonal = isPersonal;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -992627133;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -992627133;
        }
    }

    /**
     * Contains an inline keyboard layout.
     */
    public static class ReplyMarkupInlineKeyboard extends ReplyMarkup {
        /**
         * A list of rows of inline keyboard buttons.
         */
        public InlineKeyboardButton[][] rows;

        /**
         * Default constructor.
         */
        public ReplyMarkupInlineKeyboard() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param rows A list of rows of inline keyboard buttons.
         */
        public ReplyMarkupInlineKeyboard(InlineKeyboardButton[][] rows) {
            this.rows = rows;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -619317658;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -619317658;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes a text object inside an instant-view web page.
     */
    public abstract static class RichText extends Object {
    }

    /**
     * A plain text.
     */
    public static class RichTextPlain extends RichText {
        /**
         * Text.
         */
        public String text;

        /**
         * Default constructor.
         */
        public RichTextPlain() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextPlain(String text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 482617702;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 482617702;
        }
    }

    /**
     * A bold rich text.
     */
    public static class RichTextBold extends RichText {
        /**
         * Text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public RichTextBold() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextBold(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1670844268;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1670844268;
        }
    }

    /**
     * An italicized rich text.
     */
    public static class RichTextItalic extends RichText {
        /**
         * Text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public RichTextItalic() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextItalic(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1853354047;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1853354047;
        }
    }

    /**
     * An underlined rich text.
     */
    public static class RichTextUnderline extends RichText {
        /**
         * Text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public RichTextUnderline() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextUnderline(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -536019572;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -536019572;
        }
    }

    /**
     * A strike-through rich text.
     */
    public static class RichTextStrikethrough extends RichText {
        /**
         * Text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public RichTextStrikethrough() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextStrikethrough(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 723413585;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 723413585;
        }
    }

    /**
     * A fixed-width rich text.
     */
    public static class RichTextFixed extends RichText {
        /**
         * Text.
         */
        public RichText text;

        /**
         * Default constructor.
         */
        public RichTextFixed() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public RichTextFixed(RichText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1271496249;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1271496249;
        }
    }

    /**
     * A rich text URL link.
     */
    public static class RichTextUrl extends RichText {
        /**
         * Text.
         */
        public RichText text;
        /**
         * URL.
         */
        public String url;

        /**
         * Default constructor.
         */
        public RichTextUrl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         * @param url URL.
         */
        public RichTextUrl(RichText text, String url) {
            this.text = text;
            this.url = url;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1967248447;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1967248447;
        }
    }

    /**
     * A rich text email link.
     */
    public static class RichTextEmailAddress extends RichText {
        /**
         * Text.
         */
        public RichText text;
        /**
         * Email address.
         */
        public String emailAddress;

        /**
         * Default constructor.
         */
        public RichTextEmailAddress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         * @param emailAddress Email address.
         */
        public RichTextEmailAddress(RichText text, String emailAddress) {
            this.text = text;
            this.emailAddress = emailAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 40018679;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 40018679;
        }
    }

    /**
     * A concatenation of rich texts.
     */
    public static class RichTexts extends RichText {
        /**
         * Texts.
         */
        public RichText[] texts;

        /**
         * Default constructor.
         */
        public RichTexts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param texts Texts.
         */
        public RichTexts(RichText[] texts) {
            this.texts = texts;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1647457821;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1647457821;
        }
    }

    /**
     * Contains information about saved card credentials.
     */
    public static class SavedCredentials extends Object {
        /**
         * Unique identifier of the saved credentials.
         */
        public String id;
        /**
         * Title of the saved credentials.
         */
        public String title;

        /**
         * Default constructor.
         */
        public SavedCredentials() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique identifier of the saved credentials.
         * @param title Title of the saved credentials.
         */
        public SavedCredentials(String id, String title) {
            this.id = id;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -370273060;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -370273060;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a filter for message search results.
     */
    public abstract static class SearchMessagesFilter extends Object {
    }

    /**
     * Returns all found messages, no filter is applied.
     */
    public static class SearchMessagesFilterEmpty extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -869395657;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -869395657;
        }
    }

    /**
     * Returns only animation messages.
     */
    public static class SearchMessagesFilterAnimation extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterAnimation() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -155713339;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -155713339;
        }
    }

    /**
     * Returns only audio messages.
     */
    public static class SearchMessagesFilterAudio extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterAudio() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 867505275;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 867505275;
        }
    }

    /**
     * Returns only document messages.
     */
    public static class SearchMessagesFilterDocument extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterDocument() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1526331215;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1526331215;
        }
    }

    /**
     * Returns only photo messages.
     */
    public static class SearchMessagesFilterPhoto extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterPhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 925932293;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 925932293;
        }
    }

    /**
     * Returns only video messages.
     */
    public static class SearchMessagesFilterVideo extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterVideo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 115538222;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 115538222;
        }
    }

    /**
     * Returns only voice note messages.
     */
    public static class SearchMessagesFilterVoiceNote extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterVoiceNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1841439357;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1841439357;
        }
    }

    /**
     * Returns only photo and video messages.
     */
    public static class SearchMessagesFilterPhotoAndVideo extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterPhotoAndVideo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1352130963;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1352130963;
        }
    }

    /**
     * Returns only messages containing URLs.
     */
    public static class SearchMessagesFilterUrl extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterUrl() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1828724341;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1828724341;
        }
    }

    /**
     * Returns only messages containing chat photos.
     */
    public static class SearchMessagesFilterChatPhoto extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterChatPhoto() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1247751329;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1247751329;
        }
    }

    /**
     * Returns only call messages.
     */
    public static class SearchMessagesFilterCall extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterCall() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1305231012;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1305231012;
        }
    }

    /**
     * Returns only incoming call messages with missed/declined discard reasons.
     */
    public static class SearchMessagesFilterMissedCall extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterMissedCall() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 970663098;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 970663098;
        }
    }

    /**
     * Returns only video note messages.
     */
    public static class SearchMessagesFilterVideoNote extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterVideoNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 564323321;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 564323321;
        }
    }

    /**
     * Returns only voice and video note messages.
     */
    public static class SearchMessagesFilterVoiceAndVideoNote extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterVoiceAndVideoNote() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 664174819;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 664174819;
        }
    }

    /**
     * Returns only messages with mentions of the current user, or messages that are replies to their messages.
     */
    public static class SearchMessagesFilterMention extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterMention() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2001258652;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2001258652;
        }
    }

    /**
     * Returns only messages with unread mentions of the current user or messages that are replies to their messages. When using this filter the results can't be additionally filtered by a query or by the sending user.
     */
    public static class SearchMessagesFilterUnreadMention extends SearchMessagesFilter {

        /**
         * Default constructor.
         */
        public SearchMessagesFilterUnreadMention() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -95769149;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -95769149;
        }
    }

    /**
     * Represents a secret chat.
     */
    public static class SecretChat extends Object {
        /**
         * Secret chat identifier.
         */
        public int id;
        /**
         * Identifier of the chat partner.
         */
        public int userId;
        /**
         * State of the secret chat.
         */
        public SecretChatState state;
        /**
         * True, if the chat was created by the current user; otherwise false.
         */
        public boolean isOutbound;
        /**
         * Current message Time To Live setting (self-destruct timer) for the chat, in seconds.
         */
        public int ttl;
        /**
         * Hash of the currently used key for comparison with the hash of the chat partner's key. This is a string of 36 bytes, which must be used to make a 12x12 square image with a color depth of 4. The first 16 bytes should be used to make a central 8x8 square, while the remaining 20 bytes should be used to construct a 2-pixel-wide border around that square. Alternatively, the first 32 bytes of the hash can be converted to the hexadecimal format and printed as 32 2-digit hex numbers.
         */
        public byte[] keyHash;
        /**
         * Secret chat layer; determines features supported by the other client. Video notes are supported if the layer &gt;= 66.
         */
        public int layer;

        /**
         * Default constructor.
         */
        public SecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Secret chat identifier.
         * @param userId Identifier of the chat partner.
         * @param state State of the secret chat.
         * @param isOutbound True, if the chat was created by the current user; otherwise false.
         * @param ttl Current message Time To Live setting (self-destruct timer) for the chat, in seconds.
         * @param keyHash Hash of the currently used key for comparison with the hash of the chat partner's key. This is a string of 36 bytes, which must be used to make a 12x12 square image with a color depth of 4. The first 16 bytes should be used to make a central 8x8 square, while the remaining 20 bytes should be used to construct a 2-pixel-wide border around that square. Alternatively, the first 32 bytes of the hash can be converted to the hexadecimal format and printed as 32 2-digit hex numbers.
         * @param layer Secret chat layer; determines features supported by the other client. Video notes are supported if the layer &gt;= 66.
         */
        public SecretChat(int id, int userId, SecretChatState state, boolean isOutbound, int ttl, byte[] keyHash, int layer) {
            this.id = id;
            this.userId = userId;
            this.state = state;
            this.isOutbound = isOutbound;
            this.ttl = ttl;
            this.keyHash = keyHash;
            this.layer = layer;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1279231629;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1279231629;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the current secret chat state.
     */
    public abstract static class SecretChatState extends Object {
    }

    /**
     * The secret chat is not yet created; waiting for the other user to get online.
     */
    public static class SecretChatStatePending extends SecretChatState {

        /**
         * Default constructor.
         */
        public SecretChatStatePending() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1637050756;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1637050756;
        }
    }

    /**
     * The secret chat is ready to use.
     */
    public static class SecretChatStateReady extends SecretChatState {

        /**
         * Default constructor.
         */
        public SecretChatStateReady() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1611352087;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1611352087;
        }
    }

    /**
     * The secret chat is closed.
     */
    public static class SecretChatStateClosed extends SecretChatState {

        /**
         * Default constructor.
         */
        public SecretChatStateClosed() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1945106707;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1945106707;
        }
    }

    /**
     * Contains information about one session in a Telegram application used by the current user.
     */
    public static class Session extends Object {
        /**
         * Session identifier.
         */
        public long id;
        /**
         * True, if this session is the current session.
         */
        public boolean isCurrent;
        /**
         * Telegram API identifier, as provided by the application.
         */
        public int apiId;
        /**
         * Name of the application, as provided by the application.
         */
        public String applicationName;
        /**
         * The version of the application, as provided by the application.
         */
        public String applicationVersion;
        /**
         * True, if the application is an official application or uses the apiId of an official application.
         */
        public boolean isOfficialApplication;
        /**
         * Model of the device the application has been run or is running on, as provided by the application.
         */
        public String deviceModel;
        /**
         * Operating system the application has been run or is running on, as provided by the application.
         */
        public String platform;
        /**
         * Version of the operating system the application has been run or is running on, as provided by the application.
         */
        public String systemVersion;
        /**
         * Point in time (Unix timestamp) when the user has logged in.
         */
        public int logInDate;
        /**
         * Point in time (Unix timestamp) when the session was last used.
         */
        public int lastActiveDate;
        /**
         * IP address from which the session was created, in human-readable format.
         */
        public String ip;
        /**
         * A two-letter country code for the country from which the session was created, based on the IP address.
         */
        public String country;
        /**
         * Region code from which the session was created, based on the IP address.
         */
        public String region;

        /**
         * Default constructor.
         */
        public Session() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Session identifier.
         * @param isCurrent True, if this session is the current session.
         * @param apiId Telegram API identifier, as provided by the application.
         * @param applicationName Name of the application, as provided by the application.
         * @param applicationVersion The version of the application, as provided by the application.
         * @param isOfficialApplication True, if the application is an official application or uses the apiId of an official application.
         * @param deviceModel Model of the device the application has been run or is running on, as provided by the application.
         * @param platform Operating system the application has been run or is running on, as provided by the application.
         * @param systemVersion Version of the operating system the application has been run or is running on, as provided by the application.
         * @param logInDate Point in time (Unix timestamp) when the user has logged in.
         * @param lastActiveDate Point in time (Unix timestamp) when the session was last used.
         * @param ip IP address from which the session was created, in human-readable format.
         * @param country A two-letter country code for the country from which the session was created, based on the IP address.
         * @param region Region code from which the session was created, based on the IP address.
         */
        public Session(long id, boolean isCurrent, int apiId, String applicationName, String applicationVersion, boolean isOfficialApplication, String deviceModel, String platform, String systemVersion, int logInDate, int lastActiveDate, String ip, String country, String region) {
            this.id = id;
            this.isCurrent = isCurrent;
            this.apiId = apiId;
            this.applicationName = applicationName;
            this.applicationVersion = applicationVersion;
            this.isOfficialApplication = isOfficialApplication;
            this.deviceModel = deviceModel;
            this.platform = platform;
            this.systemVersion = systemVersion;
            this.logInDate = logInDate;
            this.lastActiveDate = lastActiveDate;
            this.ip = ip;
            this.country = country;
            this.region = region;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1715359000;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1715359000;
        }
    }

    /**
     * Contains a list of sessions.
     */
    public static class Sessions extends Object {
        /**
         * List of sessions.
         */
        public Session[] sessions;

        /**
         * Default constructor.
         */
        public Sessions() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sessions List of sessions.
         */
        public Sessions(Session[] sessions) {
            this.sessions = sessions;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -463118121;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -463118121;
        }
    }

    /**
     * Describes a shipping address.
     */
    public static class ShippingAddress extends Object {
        /**
         * Two-letter ISO 3166-1 alpha-2 country code.
         */
        public String countryCode;
        /**
         * State, if applicable.
         */
        public String state;
        /**
         * City.
         */
        public String city;
        /**
         * First line of the address.
         */
        public String streetLine1;
        /**
         * Second line of the address.
         */
        public String streetLine2;
        /**
         * Address postal code.
         */
        public String postalCode;

        /**
         * Default constructor.
         */
        public ShippingAddress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param countryCode Two-letter ISO 3166-1 alpha-2 country code.
         * @param state State, if applicable.
         * @param city City.
         * @param streetLine1 First line of the address.
         * @param streetLine2 Second line of the address.
         * @param postalCode Address postal code.
         */
        public ShippingAddress(String countryCode, String state, String city, String streetLine1, String streetLine2, String postalCode) {
            this.countryCode = countryCode;
            this.state = state;
            this.city = city;
            this.streetLine1 = streetLine1;
            this.streetLine2 = streetLine2;
            this.postalCode = postalCode;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 565574826;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 565574826;
        }
    }

    /**
     * One shipping option.
     */
    public static class ShippingOption extends Object {
        /**
         * Shipping option identifier.
         */
        public String id;
        /**
         * Option title.
         */
        public String title;
        /**
         * A list of objects used to calculate the total shipping costs.
         */
        public LabeledPricePart[] priceParts;

        /**
         * Default constructor.
         */
        public ShippingOption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Shipping option identifier.
         * @param title Option title.
         * @param priceParts A list of objects used to calculate the total shipping costs.
         */
        public ShippingOption(String id, String title, LabeledPricePart[] priceParts) {
            this.id = id;
            this.title = title;
            this.priceParts = priceParts;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1425690001;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1425690001;
        }
    }

    /**
     * Describes a sticker.
     */
    public static class Sticker extends Object {
        /**
         * The identifier of the sticker set to which the sticker belongs; 0 if none.
         */
        public long setId;
        /**
         * Sticker width; as defined by the sender.
         */
        public int width;
        /**
         * Sticker height; as defined by the sender.
         */
        public int height;
        /**
         * Emoji corresponding to the sticker.
         */
        public String emoji;
        /**
         * True, if the sticker is a mask.
         */
        public boolean isMask;
        /**
         * Position where the mask should be placed; may be null.
         */
        public @Nullable MaskPosition maskPosition;
        /**
         * Sticker thumbnail in WEBP or JPEG format; may be null.
         */
        public @Nullable PhotoSize thumbnail;
        /**
         * File containing the sticker.
         */
        public File sticker;

        /**
         * Default constructor.
         */
        public Sticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setId The identifier of the sticker set to which the sticker belongs; 0 if none.
         * @param width Sticker width; as defined by the sender.
         * @param height Sticker height; as defined by the sender.
         * @param emoji Emoji corresponding to the sticker.
         * @param isMask True, if the sticker is a mask.
         * @param maskPosition Position where the mask should be placed; may be null.
         * @param thumbnail Sticker thumbnail in WEBP or JPEG format; may be null.
         * @param sticker File containing the sticker.
         */
        public Sticker(long setId, int width, int height, String emoji, boolean isMask, MaskPosition maskPosition, PhotoSize thumbnail, File sticker) {
            this.setId = setId;
            this.width = width;
            this.height = height;
            this.emoji = emoji;
            this.isMask = isMask;
            this.maskPosition = maskPosition;
            this.thumbnail = thumbnail;
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -876442962;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -876442962;
        }
    }

    /**
     * Represents a list of all emoji corresponding to a sticker in a sticker set. The list is only for informational purposes, because a sticker is always sent with a fixed emoji from the corresponding Sticker object.
     */
    public static class StickerEmojis extends Object {
        /**
         * List of emojis.
         */
        public String[] emojis;

        /**
         * Default constructor.
         */
        public StickerEmojis() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param emojis List of emojis.
         */
        public StickerEmojis(String[] emojis) {
            this.emojis = emojis;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1781588570;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1781588570;
        }
    }

    /**
     * Represents a sticker set.
     */
    public static class StickerSet extends Object {
        /**
         * Identifier of the sticker set.
         */
        public long id;
        /**
         * Title of the sticker set.
         */
        public String title;
        /**
         * Name of the sticker set.
         */
        public String name;
        /**
         * True, if the sticker set has been installed by the current user.
         */
        public boolean isInstalled;
        /**
         * True, if the sticker set has been archived. A sticker set can't be installed and archived simultaneously.
         */
        public boolean isArchived;
        /**
         * True, if the sticker set is official.
         */
        public boolean isOfficial;
        /**
         * True, if the stickers in the set are masks.
         */
        public boolean isMasks;
        /**
         * True for already viewed trending sticker sets.
         */
        public boolean isViewed;
        /**
         * List of stickers in this set.
         */
        public Sticker[] stickers;
        /**
         * A list of emoji corresponding to the stickers in the same order.
         */
        public StickerEmojis[] emojis;

        /**
         * Default constructor.
         */
        public StickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Identifier of the sticker set.
         * @param title Title of the sticker set.
         * @param name Name of the sticker set.
         * @param isInstalled True, if the sticker set has been installed by the current user.
         * @param isArchived True, if the sticker set has been archived. A sticker set can't be installed and archived simultaneously.
         * @param isOfficial True, if the sticker set is official.
         * @param isMasks True, if the stickers in the set are masks.
         * @param isViewed True for already viewed trending sticker sets.
         * @param stickers List of stickers in this set.
         * @param emojis A list of emoji corresponding to the stickers in the same order.
         */
        public StickerSet(long id, String title, String name, boolean isInstalled, boolean isArchived, boolean isOfficial, boolean isMasks, boolean isViewed, Sticker[] stickers, StickerEmojis[] emojis) {
            this.id = id;
            this.title = title;
            this.name = name;
            this.isInstalled = isInstalled;
            this.isArchived = isArchived;
            this.isOfficial = isOfficial;
            this.isMasks = isMasks;
            this.isViewed = isViewed;
            this.stickers = stickers;
            this.emojis = emojis;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 72047469;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 72047469;
        }
    }

    /**
     * Represents short information about a sticker set.
     */
    public static class StickerSetInfo extends Object {
        /**
         * Identifier of the sticker set.
         */
        public long id;
        /**
         * Title of the sticker set.
         */
        public String title;
        /**
         * Name of the sticker set.
         */
        public String name;
        /**
         * True, if the sticker set has been installed by current user.
         */
        public boolean isInstalled;
        /**
         * True, if the sticker set has been archived. A sticker set can't be installed and archived simultaneously.
         */
        public boolean isArchived;
        /**
         * True, if the sticker set is official.
         */
        public boolean isOfficial;
        /**
         * True, if the stickers in the set are masks.
         */
        public boolean isMasks;
        /**
         * True for already viewed trending sticker sets.
         */
        public boolean isViewed;
        /**
         * Total number of stickers in the set.
         */
        public int size;
        /**
         * Contains up to the first 5 stickers from the set, depending on the context. If the client needs more stickers the full set should be requested.
         */
        public Sticker[] covers;

        /**
         * Default constructor.
         */
        public StickerSetInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Identifier of the sticker set.
         * @param title Title of the sticker set.
         * @param name Name of the sticker set.
         * @param isInstalled True, if the sticker set has been installed by current user.
         * @param isArchived True, if the sticker set has been archived. A sticker set can't be installed and archived simultaneously.
         * @param isOfficial True, if the sticker set is official.
         * @param isMasks True, if the stickers in the set are masks.
         * @param isViewed True for already viewed trending sticker sets.
         * @param size Total number of stickers in the set.
         * @param covers Contains up to the first 5 stickers from the set, depending on the context. If the client needs more stickers the full set should be requested.
         */
        public StickerSetInfo(long id, String title, String name, boolean isInstalled, boolean isArchived, boolean isOfficial, boolean isMasks, boolean isViewed, int size, Sticker[] covers) {
            this.id = id;
            this.title = title;
            this.name = name;
            this.isInstalled = isInstalled;
            this.isArchived = isArchived;
            this.isOfficial = isOfficial;
            this.isMasks = isMasks;
            this.isViewed = isViewed;
            this.size = size;
            this.covers = covers;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1469837113;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1469837113;
        }
    }

    /**
     * Represents a list of sticker sets.
     */
    public static class StickerSets extends Object {
        /**
         * Approximate total number of sticker sets found.
         */
        public int totalCount;
        /**
         * List of sticker sets.
         */
        public StickerSetInfo[] sets;

        /**
         * Default constructor.
         */
        public StickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param totalCount Approximate total number of sticker sets found.
         * @param sets List of sticker sets.
         */
        public StickerSets(int totalCount, StickerSetInfo[] sets) {
            this.totalCount = totalCount;
            this.sets = sets;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1883828812;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1883828812;
        }
    }

    /**
     * Represents a list of stickers.
     */
    public static class Stickers extends Object {
        /**
         * List of stickers.
         */
        public Sticker[] stickers;

        /**
         * Default constructor.
         */
        public Stickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param stickers List of stickers.
         */
        public Stickers(Sticker[] stickers) {
            this.stickers = stickers;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1974859260;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1974859260;
        }
    }

    /**
     * Contains the exact storage usage statistics split by chats and file type.
     */
    public static class StorageStatistics extends Object {
        /**
         * Total size of files.
         */
        public long size;
        /**
         * Total number of files.
         */
        public int count;
        /**
         * Statistics split by chats.
         */
        public StorageStatisticsByChat[] byChat;

        /**
         * Default constructor.
         */
        public StorageStatistics() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param size Total size of files.
         * @param count Total number of files.
         * @param byChat Statistics split by chats.
         */
        public StorageStatistics(long size, int count, StorageStatisticsByChat[] byChat) {
            this.size = size;
            this.count = count;
            this.byChat = byChat;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 217237013;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 217237013;
        }
    }

    /**
     * Contains the storage usage statistics for a specific chat.
     */
    public static class StorageStatisticsByChat extends Object {
        /**
         * Chat identifier; 0 if none.
         */
        public long chatId;
        /**
         * Total size of the files in the chat.
         */
        public long size;
        /**
         * Total number of files in the chat.
         */
        public int count;
        /**
         * Statistics split by file types.
         */
        public StorageStatisticsByFileType[] byFileType;

        /**
         * Default constructor.
         */
        public StorageStatisticsByChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier; 0 if none.
         * @param size Total size of the files in the chat.
         * @param count Total number of files in the chat.
         * @param byFileType Statistics split by file types.
         */
        public StorageStatisticsByChat(long chatId, long size, int count, StorageStatisticsByFileType[] byFileType) {
            this.chatId = chatId;
            this.size = size;
            this.count = count;
            this.byFileType = byFileType;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 635434531;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 635434531;
        }
    }

    /**
     * Contains the storage usage statistics for a specific file type.
     */
    public static class StorageStatisticsByFileType extends Object {
        /**
         * File type.
         */
        public FileType fileType;
        /**
         * Total size of the files.
         */
        public long size;
        /**
         * Total number of files.
         */
        public int count;

        /**
         * Default constructor.
         */
        public StorageStatisticsByFileType() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileType File type.
         * @param size Total size of the files.
         * @param count Total number of files.
         */
        public StorageStatisticsByFileType(FileType fileType, long size, int count) {
            this.fileType = fileType;
            this.size = size;
            this.count = count;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 714012840;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 714012840;
        }
    }

    /**
     * Contains approximate storage usage statistics, excluding files of unknown file type.
     */
    public static class StorageStatisticsFast extends Object {
        /**
         * Approximate total size of files.
         */
        public long filesSize;
        /**
         * Approximate number of files.
         */
        public int fileCount;
        /**
         * Size of the database.
         */
        public long databaseSize;

        /**
         * Default constructor.
         */
        public StorageStatisticsFast() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param filesSize Approximate total size of files.
         * @param fileCount Approximate number of files.
         * @param databaseSize Size of the database.
         */
        public StorageStatisticsFast(long filesSize, int fileCount, long databaseSize) {
            this.filesSize = filesSize;
            this.fileCount = fileCount;
            this.databaseSize = databaseSize;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2005401007;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2005401007;
        }
    }

    /**
     * Represents a supergroup or channel with zero or more members (subscribers in the case of channels). From the point of view of the system, a channel is a special kind of a supergroup: only administrators can post and see the list of members, and posts from all administrators use the name and photo of the channel instead of individual names and profile photos. Unlike supergroups, channels can have an unlimited number of subscribers.
     */
    public static class Supergroup extends Object {
        /**
         * Supergroup or channel identifier.
         */
        public int id;
        /**
         * Username of the supergroup or channel; empty for private supergroups or channels.
         */
        public String username;
        /**
         * Point in time (Unix timestamp) when the current user joined, or the point in time when the supergroup or channel was created, in case the user is not a member.
         */
        public int date;
        /**
         * Status of the current user in the supergroup or channel.
         */
        public ChatMemberStatus status;
        /**
         * Member count; 0 if unknown. Currently it is guaranteed to be known only if the supergroup or channel was found through SearchPublicChats.
         */
        public int memberCount;
        /**
         * True, if any member of the supergroup can invite other members. This field has no meaning for channels.
         */
        public boolean anyoneCanInvite;
        /**
         * True, if messages sent to the channel should contain information about the sender. This field is only applicable to channels.
         */
        public boolean signMessages;
        /**
         * True, if the supergroup is a channel.
         */
        public boolean isChannel;
        /**
         * True, if the supergroup or channel is verified.
         */
        public boolean isVerified;
        /**
         * If non-empty, contains the reason why access to this supergroup or channel must be restricted. Format of the string is &quot;{type}: {description}&quot;. {type} Contains the type of the restriction and at least one of the suffixes &quot;-all&quot;, &quot;-ios&quot;, &quot;-android&quot;, or &quot;-wp&quot;, which describe the platforms on which access should be restricted. (For example, &quot;terms-ios-android&quot;. {description} contains a human-readable description of the restriction, which can be shown to the user.)
         */
        public String restrictionReason;

        /**
         * Default constructor.
         */
        public Supergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Supergroup or channel identifier.
         * @param username Username of the supergroup or channel; empty for private supergroups or channels.
         * @param date Point in time (Unix timestamp) when the current user joined, or the point in time when the supergroup or channel was created, in case the user is not a member.
         * @param status Status of the current user in the supergroup or channel.
         * @param memberCount Member count; 0 if unknown. Currently it is guaranteed to be known only if the supergroup or channel was found through SearchPublicChats.
         * @param anyoneCanInvite True, if any member of the supergroup can invite other members. This field has no meaning for channels.
         * @param signMessages True, if messages sent to the channel should contain information about the sender. This field is only applicable to channels.
         * @param isChannel True, if the supergroup is a channel.
         * @param isVerified True, if the supergroup or channel is verified.
         * @param restrictionReason If non-empty, contains the reason why access to this supergroup or channel must be restricted. Format of the string is &quot;{type}: {description}&quot;. {type} Contains the type of the restriction and at least one of the suffixes &quot;-all&quot;, &quot;-ios&quot;, &quot;-android&quot;, or &quot;-wp&quot;, which describe the platforms on which access should be restricted. (For example, &quot;terms-ios-android&quot;. {description} contains a human-readable description of the restriction, which can be shown to the user.)
         */
        public Supergroup(int id, String username, int date, ChatMemberStatus status, int memberCount, boolean anyoneCanInvite, boolean signMessages, boolean isChannel, boolean isVerified, String restrictionReason) {
            this.id = id;
            this.username = username;
            this.date = date;
            this.status = status;
            this.memberCount = memberCount;
            this.anyoneCanInvite = anyoneCanInvite;
            this.signMessages = signMessages;
            this.isChannel = isChannel;
            this.isVerified = isVerified;
            this.restrictionReason = restrictionReason;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1737513476;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1737513476;
        }
    }

    /**
     * Contains full information about a supergroup or channel.
     */
    public static class SupergroupFullInfo extends Object {
        /**
         * Supergroup or channel description.
         */
        public String description;
        /**
         * Number of members in the supergroup or channel; 0 if unknown.
         */
        public int memberCount;
        /**
         * Number of privileged users in the supergroup or channel; 0 if unknown.
         */
        public int administratorCount;
        /**
         * Number of restricted users in the supergroup; 0 if unknown.
         */
        public int restrictedCount;
        /**
         * Number of users banned from chat; 0 if unknown.
         */
        public int bannedCount;
        /**
         * True, if members of the chat can be retrieved.
         */
        public boolean canGetMembers;
        /**
         * True, if the chat can be made public.
         */
        public boolean canSetUsername;
        /**
         * True, if the supergroup sticker set can be changed.
         */
        public boolean canSetStickerSet;
        /**
         * True, if new chat members will have access to old messages. In public supergroups and both public and private channels, old messages are always available, so this option affects only private supergroups. The value of this field is only available for chat administrators.
         */
        public boolean isAllHistoryAvailable;
        /**
         * Identifier of the supergroup sticker set; 0 if none.
         */
        public long stickerSetId;
        /**
         * Invite link for this chat.
         */
        public String inviteLink;
        /**
         * Identifier of the pinned message in the chat; 0 if none.
         */
        public long pinnedMessageId;
        /**
         * Identifier of the basic group from which supergroup was upgraded; 0 if none.
         */
        public int upgradedFromBasicGroupId;
        /**
         * Identifier of the last message in the basic group from which supergroup was upgraded; 0 if none.
         */
        public long upgradedFromMaxMessageId;

        /**
         * Default constructor.
         */
        public SupergroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param description Supergroup or channel description.
         * @param memberCount Number of members in the supergroup or channel; 0 if unknown.
         * @param administratorCount Number of privileged users in the supergroup or channel; 0 if unknown.
         * @param restrictedCount Number of restricted users in the supergroup; 0 if unknown.
         * @param bannedCount Number of users banned from chat; 0 if unknown.
         * @param canGetMembers True, if members of the chat can be retrieved.
         * @param canSetUsername True, if the chat can be made public.
         * @param canSetStickerSet True, if the supergroup sticker set can be changed.
         * @param isAllHistoryAvailable True, if new chat members will have access to old messages. In public supergroups and both public and private channels, old messages are always available, so this option affects only private supergroups. The value of this field is only available for chat administrators.
         * @param stickerSetId Identifier of the supergroup sticker set; 0 if none.
         * @param inviteLink Invite link for this chat.
         * @param pinnedMessageId Identifier of the pinned message in the chat; 0 if none.
         * @param upgradedFromBasicGroupId Identifier of the basic group from which supergroup was upgraded; 0 if none.
         * @param upgradedFromMaxMessageId Identifier of the last message in the basic group from which supergroup was upgraded; 0 if none.
         */
        public SupergroupFullInfo(String description, int memberCount, int administratorCount, int restrictedCount, int bannedCount, boolean canGetMembers, boolean canSetUsername, boolean canSetStickerSet, boolean isAllHistoryAvailable, long stickerSetId, String inviteLink, long pinnedMessageId, int upgradedFromBasicGroupId, long upgradedFromMaxMessageId) {
            this.description = description;
            this.memberCount = memberCount;
            this.administratorCount = administratorCount;
            this.restrictedCount = restrictedCount;
            this.bannedCount = bannedCount;
            this.canGetMembers = canGetMembers;
            this.canSetUsername = canSetUsername;
            this.canSetStickerSet = canSetStickerSet;
            this.isAllHistoryAvailable = isAllHistoryAvailable;
            this.stickerSetId = stickerSetId;
            this.inviteLink = inviteLink;
            this.pinnedMessageId = pinnedMessageId;
            this.upgradedFromBasicGroupId = upgradedFromBasicGroupId;
            this.upgradedFromMaxMessageId = upgradedFromMaxMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1482349223;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1482349223;
        }
    }

    /**
     * This class is an abstract base class.
     * Specifies the kind of chat members to return in getSupergroupMembers.
     */
    public abstract static class SupergroupMembersFilter extends Object {
    }

    /**
     * Returns recently active users in reverse chronological order.
     */
    public static class SupergroupMembersFilterRecent extends SupergroupMembersFilter {

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterRecent() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1178199509;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1178199509;
        }
    }

    /**
     * Returns the creator and administrators.
     */
    public static class SupergroupMembersFilterAdministrators extends SupergroupMembersFilter {

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterAdministrators() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2097380265;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2097380265;
        }
    }

    /**
     * Used to search for supergroup or channel members via a (string) query.
     */
    public static class SupergroupMembersFilterSearch extends SupergroupMembersFilter {
        /**
         * Query to search for.
         */
        public String query;

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterSearch() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         */
        public SupergroupMembersFilterSearch(String query) {
            this.query = query;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1696358469;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1696358469;
        }
    }

    /**
     * Returns restricted supergroup members; can be used only by administrators.
     */
    public static class SupergroupMembersFilterRestricted extends SupergroupMembersFilter {
        /**
         * Query to search for.
         */
        public String query;

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterRestricted() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         */
        public SupergroupMembersFilterRestricted(String query) {
            this.query = query;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1107800034;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1107800034;
        }
    }

    /**
     * Returns users banned from the supergroup or channel; can be used only by administrators.
     */
    public static class SupergroupMembersFilterBanned extends SupergroupMembersFilter {
        /**
         * Query to search for.
         */
        public String query;

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterBanned() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         */
        public SupergroupMembersFilterBanned(String query) {
            this.query = query;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1210621683;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1210621683;
        }
    }

    /**
     * Returns bot members of the supergroup or channel.
     */
    public static class SupergroupMembersFilterBots extends SupergroupMembersFilter {

        /**
         * Default constructor.
         */
        public SupergroupMembersFilterBots() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 492138918;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 492138918;
        }
    }

    /**
     * Represents a URL linking to an internal Telegram entity.
     */
    public static class TMeUrl extends Object {
        /**
         * URL.
         */
        public String url;
        /**
         * Type of the URL.
         */
        public TMeUrlType type;

        /**
         * Default constructor.
         */
        public TMeUrl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url URL.
         * @param type Type of the URL.
         */
        public TMeUrl(String url, TMeUrlType type) {
            this.url = url;
            this.type = type;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1140786622;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1140786622;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the type of a URL linking to an internal Telegram entity.
     */
    public abstract static class TMeUrlType extends Object {
    }

    /**
     * A URL linking to a user.
     */
    public static class TMeUrlTypeUser extends TMeUrlType {
        /**
         * Identifier of the user.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public TMeUrlTypeUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Identifier of the user.
         */
        public TMeUrlTypeUser(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1198700130;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1198700130;
        }
    }

    /**
     * A URL linking to a public supergroup or channel.
     */
    public static class TMeUrlTypeSupergroup extends TMeUrlType {
        /**
         * Identifier of the supergroup or channel.
         */
        public long supergroupId;

        /**
         * Default constructor.
         */
        public TMeUrlTypeSupergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         */
        public TMeUrlTypeSupergroup(long supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1353369944;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1353369944;
        }
    }

    /**
     * A chat invite link.
     */
    public static class TMeUrlTypeChatInvite extends TMeUrlType {
        /**
         * Chat invite link info.
         */
        public ChatInviteLinkInfo info;

        /**
         * Default constructor.
         */
        public TMeUrlTypeChatInvite() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param info Chat invite link info.
         */
        public TMeUrlTypeChatInvite(ChatInviteLinkInfo info) {
            this.info = info;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 313907785;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 313907785;
        }
    }

    /**
     * A URL linking to a sticker set.
     */
    public static class TMeUrlTypeStickerSet extends TMeUrlType {
        /**
         * Identifier of the sticker set.
         */
        public long stickerSetId;

        /**
         * Default constructor.
         */
        public TMeUrlTypeStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param stickerSetId Identifier of the sticker set.
         */
        public TMeUrlTypeStickerSet(long stickerSetId) {
            this.stickerSetId = stickerSetId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1602473196;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1602473196;
        }
    }

    /**
     * Contains a list of t.me URLs.
     */
    public static class TMeUrls extends Object {
        /**
         * List of URLs.
         */
        public TMeUrl[] urls;

        /**
         * Default constructor.
         */
        public TMeUrls() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param urls List of URLs.
         */
        public TMeUrls(TMeUrl[] urls) {
            this.urls = urls;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1130595098;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1130595098;
        }
    }

    /**
     * Contains parameters for TDLib initialization.
     */
    public static class TdlibParameters extends Object {
        /**
         * If set to true, the Telegram test environment will be used instead of the production environment.
         */
        public boolean useTestDc;
        /**
         * The path to the directory for the persistent database; if empty, the current working directory will be used.
         */
        public String databaseDirectory;
        /**
         * The path to the directory for storing files; if empty, databaseDirectory will be used.
         */
        public String filesDirectory;
        /**
         * If set to true, information about downloaded and uploaded files will be saved between application restarts.
         */
        public boolean useFileDatabase;
        /**
         * If set to true, the library will maintain a cache of users, basic groups, supergroups, channels and secret chats. Implies useFileDatabase.
         */
        public boolean useChatInfoDatabase;
        /**
         * If set to true, the library will maintain a cache of chats and messages. Implies useChatInfoDatabase.
         */
        public boolean useMessageDatabase;
        /**
         * If set to true, support for secret chats will be enabled.
         */
        public boolean useSecretChats;
        /**
         * Application identifier for Telegram API access, which can be obtained at https://my.telegram.org.
         */
        public int apiId;
        /**
         * Application identifier hash for Telegram API access, which can be obtained at https://my.telegram.org.
         */
        public String apiHash;
        /**
         * IETF language tag of the user's operating system language; must be non-empty.
         */
        public String systemLanguageCode;
        /**
         * Model of the device the application is being run on; must be non-empty.
         */
        public String deviceModel;
        /**
         * Version of the operating system the application is being run on; must be non-empty.
         */
        public String systemVersion;
        /**
         * Application version; must be non-empty.
         */
        public String applicationVersion;
        /**
         * If set to true, old files will automatically be deleted.
         */
        public boolean enableStorageOptimizer;
        /**
         * If set to true, original file names will be ignored. Otherwise, downloaded files will be saved under names as close as possible to the original name.
         */
        public boolean ignoreFileNames;

        /**
         * Default constructor.
         */
        public TdlibParameters() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param useTestDc If set to true, the Telegram test environment will be used instead of the production environment.
         * @param databaseDirectory The path to the directory for the persistent database; if empty, the current working directory will be used.
         * @param filesDirectory The path to the directory for storing files; if empty, databaseDirectory will be used.
         * @param useFileDatabase If set to true, information about downloaded and uploaded files will be saved between application restarts.
         * @param useChatInfoDatabase If set to true, the library will maintain a cache of users, basic groups, supergroups, channels and secret chats. Implies useFileDatabase.
         * @param useMessageDatabase If set to true, the library will maintain a cache of chats and messages. Implies useChatInfoDatabase.
         * @param useSecretChats If set to true, support for secret chats will be enabled.
         * @param apiId Application identifier for Telegram API access, which can be obtained at https://my.telegram.org.
         * @param apiHash Application identifier hash for Telegram API access, which can be obtained at https://my.telegram.org.
         * @param systemLanguageCode IETF language tag of the user's operating system language; must be non-empty.
         * @param deviceModel Model of the device the application is being run on; must be non-empty.
         * @param systemVersion Version of the operating system the application is being run on; must be non-empty.
         * @param applicationVersion Application version; must be non-empty.
         * @param enableStorageOptimizer If set to true, old files will automatically be deleted.
         * @param ignoreFileNames If set to true, original file names will be ignored. Otherwise, downloaded files will be saved under names as close as possible to the original name.
         */
        public TdlibParameters(boolean useTestDc, String databaseDirectory, String filesDirectory, boolean useFileDatabase, boolean useChatInfoDatabase, boolean useMessageDatabase, boolean useSecretChats, int apiId, String apiHash, String systemLanguageCode, String deviceModel, String systemVersion, String applicationVersion, boolean enableStorageOptimizer, boolean ignoreFileNames) {
            this.useTestDc = useTestDc;
            this.databaseDirectory = databaseDirectory;
            this.filesDirectory = filesDirectory;
            this.useFileDatabase = useFileDatabase;
            this.useChatInfoDatabase = useChatInfoDatabase;
            this.useMessageDatabase = useMessageDatabase;
            this.useSecretChats = useSecretChats;
            this.apiId = apiId;
            this.apiHash = apiHash;
            this.systemLanguageCode = systemLanguageCode;
            this.deviceModel = deviceModel;
            this.systemVersion = systemVersion;
            this.applicationVersion = applicationVersion;
            this.enableStorageOptimizer = enableStorageOptimizer;
            this.ignoreFileNames = ignoreFileNames;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -761520773;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -761520773;
        }
    }

    /**
     * Returns information about the availability of a temporary password, which can be used for payments.
     */
    public static class TemporaryPasswordState extends Object {
        /**
         * True, if a temporary password is available.
         */
        public boolean hasPassword;
        /**
         * Time left before the temporary password expires, in seconds.
         */
        public int validFor;

        /**
         * Default constructor.
         */
        public TemporaryPasswordState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param hasPassword True, if a temporary password is available.
         * @param validFor Time left before the temporary password expires, in seconds.
         */
        public TemporaryPasswordState(boolean hasPassword, int validFor) {
            this.hasPassword = hasPassword;
            this.validFor = validFor;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 939837410;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 939837410;
        }
    }

    /**
     * A simple object containing a sequence of bytes; for testing only.
     */
    public static class TestBytes extends Object {
        /**
         * Bytes.
         */
        public byte[] value;

        /**
         * Default constructor.
         */
        public TestBytes() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Bytes.
         */
        public TestBytes(byte[] value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1541225250;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1541225250;
        }
    }

    /**
     * A simple object containing a number; for testing only.
     */
    public static class TestInt extends Object {
        /**
         * Number.
         */
        public int value;

        /**
         * Default constructor.
         */
        public TestInt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Number.
         */
        public TestInt(int value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -574804983;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -574804983;
        }
    }

    /**
     * A simple object containing a string; for testing only.
     */
    public static class TestString extends Object {
        /**
         * String.
         */
        public String value;

        /**
         * Default constructor.
         */
        public TestString() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value String.
         */
        public TestString(String value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -27891572;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -27891572;
        }
    }

    /**
     * A simple object containing a vector of numbers; for testing only.
     */
    public static class TestVectorInt extends Object {
        /**
         * Vector of numbers.
         */
        public int[] value;

        /**
         * Default constructor.
         */
        public TestVectorInt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Vector of numbers.
         */
        public TestVectorInt(int[] value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 593682027;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 593682027;
        }
    }

    /**
     * A simple object containing a vector of objects that hold a number; for testing only.
     */
    public static class TestVectorIntObject extends Object {
        /**
         * Vector of objects.
         */
        public TestInt[] value;

        /**
         * Default constructor.
         */
        public TestVectorIntObject() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Vector of objects.
         */
        public TestVectorIntObject(TestInt[] value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 125891546;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 125891546;
        }
    }

    /**
     * A simple object containing a vector of strings; for testing only.
     */
    public static class TestVectorString extends Object {
        /**
         * Vector of strings.
         */
        public String[] value;

        /**
         * Default constructor.
         */
        public TestVectorString() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Vector of strings.
         */
        public TestVectorString(String[] value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 79339995;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 79339995;
        }
    }

    /**
     * A simple object containing a vector of objects that hold a string; for testing only.
     */
    public static class TestVectorStringObject extends Object {
        /**
         * Vector of objects.
         */
        public TestString[] value;

        /**
         * Default constructor.
         */
        public TestVectorStringObject() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param value Vector of objects.
         */
        public TestVectorStringObject(TestString[] value) {
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 80780537;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 80780537;
        }
    }

    /**
     * Contains some text.
     */
    public static class Text extends Object {
        /**
         * Text.
         */
        public String text;

        /**
         * Default constructor.
         */
        public Text() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Text.
         */
        public Text(String text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 578181272;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 578181272;
        }
    }

    /**
     * Contains a list of text entities.
     */
    public static class TextEntities extends Object {
        /**
         * List of text entities.
         */
        public TextEntity[] entities;

        /**
         * Default constructor.
         */
        public TextEntities() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param entities List of text entities.
         */
        public TextEntities(TextEntity[] entities) {
            this.entities = entities;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -933199172;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -933199172;
        }
    }

    /**
     * Represents a part of the text that needs to be formatted in some unusual way.
     */
    public static class TextEntity extends Object {
        /**
         * Offset of the entity in UTF-16 code points.
         */
        public int offset;
        /**
         * Length of the entity, in UTF-16 code points.
         */
        public int length;
        /**
         * Type of the entity.
         */
        public TextEntityType type;

        /**
         * Default constructor.
         */
        public TextEntity() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param offset Offset of the entity in UTF-16 code points.
         * @param length Length of the entity, in UTF-16 code points.
         * @param type Type of the entity.
         */
        public TextEntity(int offset, int length, TextEntityType type) {
            this.offset = offset;
            this.length = length;
            this.type = type;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1951688280;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1951688280;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a part of the text which must be formatted differently.
     */
    public abstract static class TextEntityType extends Object {
    }

    /**
     * A mention of a user by their username.
     */
    public static class TextEntityTypeMention extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeMention() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 934535013;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 934535013;
        }
    }

    /**
     * A hashtag text, beginning with &quot;#&quot;.
     */
    public static class TextEntityTypeHashtag extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeHashtag() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1023958307;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1023958307;
        }
    }

    /**
     * A cashtag text, beginning with &quot;$&quot; and consisting of capital english letters (i.e. &quot;$USD&quot;).
     */
    public static class TextEntityTypeCashtag extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeCashtag() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1222915915;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1222915915;
        }
    }

    /**
     * A bot command, beginning with &quot;/&quot;. This shouldn't be highlighted if there are no bots in the chat.
     */
    public static class TextEntityTypeBotCommand extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeBotCommand() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1150997581;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1150997581;
        }
    }

    /**
     * An HTTP URL.
     */
    public static class TextEntityTypeUrl extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeUrl() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1312762756;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1312762756;
        }
    }

    /**
     * An email address.
     */
    public static class TextEntityTypeEmailAddress extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeEmailAddress() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1425545249;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1425545249;
        }
    }

    /**
     * A bold text.
     */
    public static class TextEntityTypeBold extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeBold() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1128210000;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1128210000;
        }
    }

    /**
     * An italic text.
     */
    public static class TextEntityTypeItalic extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeItalic() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -118253987;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -118253987;
        }
    }

    /**
     * Text that must be formatted as if inside a code HTML tag.
     */
    public static class TextEntityTypeCode extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypeCode() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -974534326;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -974534326;
        }
    }

    /**
     * Text that must be formatted as if inside a pre HTML tag.
     */
    public static class TextEntityTypePre extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypePre() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1648958606;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1648958606;
        }
    }

    /**
     * Text that must be formatted as if inside pre, and code HTML tags.
     */
    public static class TextEntityTypePreCode extends TextEntityType {
        /**
         * Programming language of the code; as defined by the sender.
         */
        public String language;

        /**
         * Default constructor.
         */
        public TextEntityTypePreCode() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param language Programming language of the code; as defined by the sender.
         */
        public TextEntityTypePreCode(String language) {
            this.language = language;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -945325397;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -945325397;
        }
    }

    /**
     * A text description shown instead of a raw URL.
     */
    public static class TextEntityTypeTextUrl extends TextEntityType {
        /**
         * URL to be opened when the link is clicked.
         */
        public String url;

        /**
         * Default constructor.
         */
        public TextEntityTypeTextUrl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url URL to be opened when the link is clicked.
         */
        public TextEntityTypeTextUrl(String url) {
            this.url = url;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 445719651;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 445719651;
        }
    }

    /**
     * A text shows instead of a raw mention of the user (e.g., when the user has no username).
     */
    public static class TextEntityTypeMentionName extends TextEntityType {
        /**
         * Identifier of the mentioned user.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public TextEntityTypeMentionName() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Identifier of the mentioned user.
         */
        public TextEntityTypeMentionName(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -791517091;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -791517091;
        }
    }

    /**
     * A phone number.
     */
    public static class TextEntityTypePhoneNumber extends TextEntityType {

        /**
         * Default constructor.
         */
        public TextEntityTypePhoneNumber() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1160140246;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1160140246;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the way the text should be parsed for TextEntities.
     */
    public abstract static class TextParseMode extends Object {
    }

    /**
     * The text should be parsed in markdown-style.
     */
    public static class TextParseModeMarkdown extends TextParseMode {

        /**
         * Default constructor.
         */
        public TextParseModeMarkdown() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 969225580;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 969225580;
        }
    }

    /**
     * The text should be parsed in HTML-style.
     */
    public static class TextParseModeHTML extends TextParseMode {

        /**
         * Default constructor.
         */
        public TextParseModeHTML() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1660208627;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1660208627;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the categories of chats for which a list of frequently used chats can be retrieved.
     */
    public abstract static class TopChatCategory extends Object {
    }

    /**
     * A category containing frequently used private chats with non-bot users.
     */
    public static class TopChatCategoryUsers extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryUsers() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1026706816;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1026706816;
        }
    }

    /**
     * A category containing frequently used private chats with bot users.
     */
    public static class TopChatCategoryBots extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryBots() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1577129195;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1577129195;
        }
    }

    /**
     * A category containing frequently used basic groups and supergroups.
     */
    public static class TopChatCategoryGroups extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryGroups() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1530056846;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1530056846;
        }
    }

    /**
     * A category containing frequently used channels.
     */
    public static class TopChatCategoryChannels extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryChannels() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -500825885;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -500825885;
        }
    }

    /**
     * A category containing frequently used chats with inline bots sorted by their usage in inline mode.
     */
    public static class TopChatCategoryInlineBots extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryInlineBots() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 377023356;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 377023356;
        }
    }

    /**
     * A category containing frequently used chats used for calls.
     */
    public static class TopChatCategoryCalls extends TopChatCategory {

        /**
         * Default constructor.
         */
        public TopChatCategoryCalls() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 356208861;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 356208861;
        }
    }

    /**
     * This class is an abstract base class.
     * Contains notifications about data changes.
     */
    public abstract static class Update extends Object {
    }

    /**
     * The user authorization state has changed.
     */
    public static class UpdateAuthorizationState extends Update {
        /**
         * New authorization state.
         */
        public AuthorizationState authorizationState;

        /**
         * Default constructor.
         */
        public UpdateAuthorizationState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param authorizationState New authorization state.
         */
        public UpdateAuthorizationState(AuthorizationState authorizationState) {
            this.authorizationState = authorizationState;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1622347490;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1622347490;
        }
    }

    /**
     * A new message was received; can also be an outgoing message.
     */
    public static class UpdateNewMessage extends Update {
        /**
         * The new message.
         */
        public Message message;
        /**
         * True, if this message must not generate a notification.
         */
        public boolean disableNotification;
        /**
         * True, if the message contains a mention of the current user.
         */
        public boolean containsMention;

        /**
         * Default constructor.
         */
        public UpdateNewMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param message The new message.
         * @param disableNotification True, if this message must not generate a notification.
         * @param containsMention True, if the message contains a mention of the current user.
         */
        public UpdateNewMessage(Message message, boolean disableNotification, boolean containsMention) {
            this.message = message;
            this.disableNotification = disableNotification;
            this.containsMention = containsMention;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 238944219;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 238944219;
        }
    }

    /**
     * A request to send a message has reached the Telegram server. This doesn't mean that the message will be sent successfully or even that the send message request will be processed. This update will be sent only if the option &quot;useQuickAck&quot; is set to true. This update may be sent multiple times for the same message.
     */
    public static class UpdateMessageSendAcknowledged extends Update {
        /**
         * The chat identifier of the sent message.
         */
        public long chatId;
        /**
         * A temporary message identifier.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public UpdateMessageSendAcknowledged() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat identifier of the sent message.
         * @param messageId A temporary message identifier.
         */
        public UpdateMessageSendAcknowledged(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1302843961;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1302843961;
        }
    }

    /**
     * A message has been successfully sent.
     */
    public static class UpdateMessageSendSucceeded extends Update {
        /**
         * Information about the sent message. Usually only the message identifier, date, and content are changed, but almost all other fields can also change.
         */
        public Message message;
        /**
         * The previous temporary message identifier.
         */
        public long oldMessageId;

        /**
         * Default constructor.
         */
        public UpdateMessageSendSucceeded() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param message Information about the sent message. Usually only the message identifier, date, and content are changed, but almost all other fields can also change.
         * @param oldMessageId The previous temporary message identifier.
         */
        public UpdateMessageSendSucceeded(Message message, long oldMessageId) {
            this.message = message;
            this.oldMessageId = oldMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1815715197;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1815715197;
        }
    }

    /**
     * A message failed to send. Be aware that some messages being sent can be irrecoverably deleted, in which case updateDeleteMessages will be received instead of this update.
     */
    public static class UpdateMessageSendFailed extends Update {
        /**
         * Contains information about the message that failed to send.
         */
        public Message message;
        /**
         * The previous temporary message identifier.
         */
        public long oldMessageId;
        /**
         * An error code.
         */
        public int errorCode;
        /**
         * Error message.
         */
        public String errorMessage;

        /**
         * Default constructor.
         */
        public UpdateMessageSendFailed() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param message Contains information about the message that failed to send.
         * @param oldMessageId The previous temporary message identifier.
         * @param errorCode An error code.
         * @param errorMessage Error message.
         */
        public UpdateMessageSendFailed(Message message, long oldMessageId, int errorCode, String errorMessage) {
            this.message = message;
            this.oldMessageId = oldMessageId;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1032335779;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1032335779;
        }
    }

    /**
     * The message content has changed.
     */
    public static class UpdateMessageContent extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * New message content.
         */
        public MessageContent newContent;

        /**
         * Default constructor.
         */
        public UpdateMessageContent() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId Message identifier.
         * @param newContent New message content.
         */
        public UpdateMessageContent(long chatId, long messageId, MessageContent newContent) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.newContent = newContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 506903332;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 506903332;
        }
    }

    /**
     * A message was edited. Changes in the message content will come in a separate updateMessageContent.
     */
    public static class UpdateMessageEdited extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * Point in time (Unix timestamp) when the message was edited.
         */
        public int editDate;
        /**
         * New message reply markup; may be null.
         */
        public @Nullable ReplyMarkup replyMarkup;

        /**
         * Default constructor.
         */
        public UpdateMessageEdited() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId Message identifier.
         * @param editDate Point in time (Unix timestamp) when the message was edited.
         * @param replyMarkup New message reply markup; may be null.
         */
        public UpdateMessageEdited(long chatId, long messageId, int editDate, ReplyMarkup replyMarkup) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.editDate = editDate;
            this.replyMarkup = replyMarkup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -559545626;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -559545626;
        }
    }

    /**
     * The view count of the message has changed.
     */
    public static class UpdateMessageViews extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * New value of the view count.
         */
        public int views;

        /**
         * Default constructor.
         */
        public UpdateMessageViews() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId Message identifier.
         * @param views New value of the view count.
         */
        public UpdateMessageViews(long chatId, long messageId, int views) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.views = views;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1854131125;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1854131125;
        }
    }

    /**
     * The message content was opened. Updates voice note messages to &quot;listened&quot;, video note messages to &quot;viewed&quot; and starts the TTL timer for self-destructing messages.
     */
    public static class UpdateMessageContentOpened extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public UpdateMessageContentOpened() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId Message identifier.
         */
        public UpdateMessageContentOpened(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1520523131;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1520523131;
        }
    }

    /**
     * A message with an unread mention was read.
     */
    public static class UpdateMessageMentionRead extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * The new number of unread mention messages left in the chat.
         */
        public int unreadMentionCount;

        /**
         * Default constructor.
         */
        public UpdateMessageMentionRead() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId Message identifier.
         * @param unreadMentionCount The new number of unread mention messages left in the chat.
         */
        public UpdateMessageMentionRead(long chatId, long messageId, int unreadMentionCount) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.unreadMentionCount = unreadMentionCount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -252228282;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -252228282;
        }
    }

    /**
     * A new chat has been loaded/created. This update is guaranteed to come before the chat identifier is returned to the client. The chat field changes will be reported through separate updates.
     */
    public static class UpdateNewChat extends Update {
        /**
         * The chat.
         */
        public Chat chat;

        /**
         * Default constructor.
         */
        public UpdateNewChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chat The chat.
         */
        public UpdateNewChat(Chat chat) {
            this.chat = chat;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2075757773;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2075757773;
        }
    }

    /**
     * The title of a chat was changed.
     */
    public static class UpdateChatTitle extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The new chat title.
         */
        public String title;

        /**
         * Default constructor.
         */
        public UpdateChatTitle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param title The new chat title.
         */
        public UpdateChatTitle(long chatId, String title) {
            this.chatId = chatId;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -175405660;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -175405660;
        }
    }

    /**
     * A chat photo was changed.
     */
    public static class UpdateChatPhoto extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The new chat photo; may be null.
         */
        public @Nullable ChatPhoto photo;

        /**
         * Default constructor.
         */
        public UpdateChatPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param photo The new chat photo; may be null.
         */
        public UpdateChatPhoto(long chatId, ChatPhoto photo) {
            this.chatId = chatId;
            this.photo = photo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -209353966;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -209353966;
        }
    }

    /**
     * The last message of a chat was changed. If lastMessage is null then the last message in the chat became unknown. Some new unknown messages might be added to the chat in this case.
     */
    public static class UpdateChatLastMessage extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The new last message in the chat; may be null.
         */
        public @Nullable Message lastMessage;
        /**
         * New value of the chat order.
         */
        public long order;

        /**
         * Default constructor.
         */
        public UpdateChatLastMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param lastMessage The new last message in the chat; may be null.
         * @param order New value of the chat order.
         */
        public UpdateChatLastMessage(long chatId, Message lastMessage, long order) {
            this.chatId = chatId;
            this.lastMessage = lastMessage;
            this.order = order;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 580348828;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 580348828;
        }
    }

    /**
     * The order of the chat in the chats list has changed. Instead of this update updateChatLastMessage, updateChatIsPinned or updateChatDraftMessage might be sent.
     */
    public static class UpdateChatOrder extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New value of the order.
         */
        public long order;

        /**
         * Default constructor.
         */
        public UpdateChatOrder() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param order New value of the order.
         */
        public UpdateChatOrder(long chatId, long order) {
            this.chatId = chatId;
            this.order = order;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1601888026;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1601888026;
        }
    }

    /**
     * A chat was pinned or unpinned.
     */
    public static class UpdateChatIsPinned extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New value of isPinned.
         */
        public boolean isPinned;
        /**
         * New value of the chat order.
         */
        public long order;

        /**
         * Default constructor.
         */
        public UpdateChatIsPinned() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param isPinned New value of isPinned.
         * @param order New value of the chat order.
         */
        public UpdateChatIsPinned(long chatId, boolean isPinned, long order) {
            this.chatId = chatId;
            this.isPinned = isPinned;
            this.order = order;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 488876260;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 488876260;
        }
    }

    /**
     * Incoming messages were read or number of unread messages has been changed.
     */
    public static class UpdateChatReadInbox extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of the last read incoming message.
         */
        public long lastReadInboxMessageId;
        /**
         * The number of unread messages left in the chat.
         */
        public int unreadCount;

        /**
         * Default constructor.
         */
        public UpdateChatReadInbox() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param lastReadInboxMessageId Identifier of the last read incoming message.
         * @param unreadCount The number of unread messages left in the chat.
         */
        public UpdateChatReadInbox(long chatId, long lastReadInboxMessageId, int unreadCount) {
            this.chatId = chatId;
            this.lastReadInboxMessageId = lastReadInboxMessageId;
            this.unreadCount = unreadCount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -797952281;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -797952281;
        }
    }

    /**
     * Outgoing messages were read.
     */
    public static class UpdateChatReadOutbox extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of last read outgoing message.
         */
        public long lastReadOutboxMessageId;

        /**
         * Default constructor.
         */
        public UpdateChatReadOutbox() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param lastReadOutboxMessageId Identifier of last read outgoing message.
         */
        public UpdateChatReadOutbox(long chatId, long lastReadOutboxMessageId) {
            this.chatId = chatId;
            this.lastReadOutboxMessageId = lastReadOutboxMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 708334213;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 708334213;
        }
    }

    /**
     * The chat unreadMentionCount has changed.
     */
    public static class UpdateChatUnreadMentionCount extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The number of unread mention messages left in the chat.
         */
        public int unreadMentionCount;

        /**
         * Default constructor.
         */
        public UpdateChatUnreadMentionCount() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param unreadMentionCount The number of unread mention messages left in the chat.
         */
        public UpdateChatUnreadMentionCount(long chatId, int unreadMentionCount) {
            this.chatId = chatId;
            this.unreadMentionCount = unreadMentionCount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2131461348;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2131461348;
        }
    }

    /**
     * Notification settings for some chats were updated.
     */
    public static class UpdateNotificationSettings extends Update {
        /**
         * Types of chats for which notification settings were updated.
         */
        public NotificationSettingsScope scope;
        /**
         * The new notification settings.
         */
        public NotificationSettings notificationSettings;

        /**
         * Default constructor.
         */
        public UpdateNotificationSettings() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param scope Types of chats for which notification settings were updated.
         * @param notificationSettings The new notification settings.
         */
        public UpdateNotificationSettings(NotificationSettingsScope scope, NotificationSettings notificationSettings) {
            this.scope = scope;
            this.notificationSettings = notificationSettings;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1767306883;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1767306883;
        }
    }

    /**
     * The default chat reply markup was changed. Can occur because new messages with reply markup were received or because an old reply markup was hidden by the user.
     */
    public static class UpdateChatReplyMarkup extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of the message from which reply markup needs to be used; 0 if there is no default custom reply markup in the chat.
         */
        public long replyMarkupMessageId;

        /**
         * Default constructor.
         */
        public UpdateChatReplyMarkup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param replyMarkupMessageId Identifier of the message from which reply markup needs to be used; 0 if there is no default custom reply markup in the chat.
         */
        public UpdateChatReplyMarkup(long chatId, long replyMarkupMessageId) {
            this.chatId = chatId;
            this.replyMarkupMessageId = replyMarkupMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1309386144;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1309386144;
        }
    }

    /**
     * A draft has changed. Be aware that the update may come in the currently opened chat but with old content of the draft. If the user has changed the content of the draft, this update shouldn't be applied.
     */
    public static class UpdateChatDraftMessage extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The new draft message; may be null.
         */
        public @Nullable DraftMessage draftMessage;
        /**
         * New value of the chat order.
         */
        public long order;

        /**
         * Default constructor.
         */
        public UpdateChatDraftMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param draftMessage The new draft message; may be null.
         * @param order New value of the chat order.
         */
        public UpdateChatDraftMessage(long chatId, DraftMessage draftMessage, long order) {
            this.chatId = chatId;
            this.draftMessage = draftMessage;
            this.order = order;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1436617498;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1436617498;
        }
    }

    /**
     * Some messages were deleted.
     */
    public static class UpdateDeleteMessages extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifiers of the deleted messages.
         */
        public long[] messageIds;
        /**
         * True, if the messages are permanently deleted by a user (as opposed to just becoming unaccessible).
         */
        public boolean isPermanent;
        /**
         * True, if the messages are deleted only from the cache and can possibly be retrieved again in the future.
         */
        public boolean fromCache;

        /**
         * Default constructor.
         */
        public UpdateDeleteMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageIds Identifiers of the deleted messages.
         * @param isPermanent True, if the messages are permanently deleted by a user (as opposed to just becoming unaccessible).
         * @param fromCache True, if the messages are deleted only from the cache and can possibly be retrieved again in the future.
         */
        public UpdateDeleteMessages(long chatId, long[] messageIds, boolean isPermanent, boolean fromCache) {
            this.chatId = chatId;
            this.messageIds = messageIds;
            this.isPermanent = isPermanent;
            this.fromCache = fromCache;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1669252686;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1669252686;
        }
    }

    /**
     * User activity in the chat has changed.
     */
    public static class UpdateUserChatAction extends Update {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of a user performing an action.
         */
        public int userId;
        /**
         * The action description.
         */
        public ChatAction action;

        /**
         * Default constructor.
         */
        public UpdateUserChatAction() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userId Identifier of a user performing an action.
         * @param action The action description.
         */
        public UpdateUserChatAction(long chatId, int userId, ChatAction action) {
            this.chatId = chatId;
            this.userId = userId;
            this.action = action;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1444133514;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1444133514;
        }
    }

    /**
     * The user went online or offline.
     */
    public static class UpdateUserStatus extends Update {
        /**
         * User identifier.
         */
        public int userId;
        /**
         * New status of the user.
         */
        public UserStatus status;

        /**
         * Default constructor.
         */
        public UpdateUserStatus() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         * @param status New status of the user.
         */
        public UpdateUserStatus(int userId, UserStatus status) {
            this.userId = userId;
            this.status = status;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1443545195;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1443545195;
        }
    }

    /**
     * Some data of a user has changed. This update is guaranteed to come before the user identifier is returned to the client.
     */
    public static class UpdateUser extends Update {
        /**
         * New data about the user.
         */
        public User user;

        /**
         * Default constructor.
         */
        public UpdateUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param user New data about the user.
         */
        public UpdateUser(User user) {
            this.user = user;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1183394041;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1183394041;
        }
    }

    /**
     * Some data of a basic group has changed. This update is guaranteed to come before the basic group identifier is returned to the client.
     */
    public static class UpdateBasicGroup extends Update {
        /**
         * New data about the group.
         */
        public BasicGroup basicGroup;

        /**
         * Default constructor.
         */
        public UpdateBasicGroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroup New data about the group.
         */
        public UpdateBasicGroup(BasicGroup basicGroup) {
            this.basicGroup = basicGroup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1003239581;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1003239581;
        }
    }

    /**
     * Some data of a supergroup or a channel has changed. This update is guaranteed to come before the supergroup identifier is returned to the client.
     */
    public static class UpdateSupergroup extends Update {
        /**
         * New data about the supergroup.
         */
        public Supergroup supergroup;

        /**
         * Default constructor.
         */
        public UpdateSupergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroup New data about the supergroup.
         */
        public UpdateSupergroup(Supergroup supergroup) {
            this.supergroup = supergroup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -76782300;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -76782300;
        }
    }

    /**
     * Some data of a secret chat has changed. This update is guaranteed to come before the secret chat identifier is returned to the client.
     */
    public static class UpdateSecretChat extends Update {
        /**
         * New data about the secret chat.
         */
        public SecretChat secretChat;

        /**
         * Default constructor.
         */
        public UpdateSecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param secretChat New data about the secret chat.
         */
        public UpdateSecretChat(SecretChat secretChat) {
            this.secretChat = secretChat;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1666903253;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1666903253;
        }
    }

    /**
     * Some data from userFullInfo has been changed.
     */
    public static class UpdateUserFullInfo extends Update {
        /**
         * User identifier.
         */
        public int userId;
        /**
         * New full information about the user.
         */
        public UserFullInfo userFullInfo;

        /**
         * Default constructor.
         */
        public UpdateUserFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         * @param userFullInfo New full information about the user.
         */
        public UpdateUserFullInfo(int userId, UserFullInfo userFullInfo) {
            this.userId = userId;
            this.userFullInfo = userFullInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 222103874;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 222103874;
        }
    }

    /**
     * Some data from basicGroupFullInfo has been changed.
     */
    public static class UpdateBasicGroupFullInfo extends Update {
        /**
         * Identifier of a basic group.
         */
        public int basicGroupId;
        /**
         * New full information about the group.
         */
        public BasicGroupFullInfo basicGroupFullInfo;

        /**
         * Default constructor.
         */
        public UpdateBasicGroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Identifier of a basic group.
         * @param basicGroupFullInfo New full information about the group.
         */
        public UpdateBasicGroupFullInfo(int basicGroupId, BasicGroupFullInfo basicGroupFullInfo) {
            this.basicGroupId = basicGroupId;
            this.basicGroupFullInfo = basicGroupFullInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 924030531;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 924030531;
        }
    }

    /**
     * Some data from supergroupFullInfo has been changed.
     */
    public static class UpdateSupergroupFullInfo extends Update {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;
        /**
         * New full information about the supergroup.
         */
        public SupergroupFullInfo supergroupFullInfo;

        /**
         * Default constructor.
         */
        public UpdateSupergroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         * @param supergroupFullInfo New full information about the supergroup.
         */
        public UpdateSupergroupFullInfo(int supergroupId, SupergroupFullInfo supergroupFullInfo) {
            this.supergroupId = supergroupId;
            this.supergroupFullInfo = supergroupFullInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1288828758;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1288828758;
        }
    }

    /**
     * Service notification from the server. Upon receiving this the client must show a popup with the content of the notification.
     */
    public static class UpdateServiceNotification extends Update {
        /**
         * Notification type.
         */
        public String type;
        /**
         * Notification content.
         */
        public MessageContent content;

        /**
         * Default constructor.
         */
        public UpdateServiceNotification() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param type Notification type.
         * @param content Notification content.
         */
        public UpdateServiceNotification(String type, MessageContent content) {
            this.type = type;
            this.content = content;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1318622637;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1318622637;
        }
    }

    /**
     * Information about a file was updated.
     */
    public static class UpdateFile extends Update {
        /**
         * New data about the file.
         */
        public File file;

        /**
         * Default constructor.
         */
        public UpdateFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param file New data about the file.
         */
        public UpdateFile(File file) {
            this.file = file;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 114132831;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 114132831;
        }
    }

    /**
     * The file generation process needs to be started by the client.
     */
    public static class UpdateFileGenerationStart extends Update {
        /**
         * Unique identifier for the generation process.
         */
        public long generationId;
        /**
         * The path to a file from which a new file is generated, may be empty.
         */
        public String originalPath;
        /**
         * The path to a file that should be created and where the new file should be generated.
         */
        public String destinationPath;
        /**
         * String specifying the conversion applied to the original file. If conversion is &quot;#url#&quot; than originalPath contains a HTTP/HTTPS URL of a file, which should be downloaded by the client.
         */
        public String conversion;

        /**
         * Default constructor.
         */
        public UpdateFileGenerationStart() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param generationId Unique identifier for the generation process.
         * @param originalPath The path to a file from which a new file is generated, may be empty.
         * @param destinationPath The path to a file that should be created and where the new file should be generated.
         * @param conversion String specifying the conversion applied to the original file. If conversion is &quot;#url#&quot; than originalPath contains a HTTP/HTTPS URL of a file, which should be downloaded by the client.
         */
        public UpdateFileGenerationStart(long generationId, String originalPath, String destinationPath, String conversion) {
            this.generationId = generationId;
            this.originalPath = originalPath;
            this.destinationPath = destinationPath;
            this.conversion = conversion;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 216817388;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 216817388;
        }
    }

    /**
     * File generation is no longer needed.
     */
    public static class UpdateFileGenerationStop extends Update {
        /**
         * Unique identifier for the generation process.
         */
        public long generationId;

        /**
         * Default constructor.
         */
        public UpdateFileGenerationStop() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param generationId Unique identifier for the generation process.
         */
        public UpdateFileGenerationStop(long generationId) {
            this.generationId = generationId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1894449685;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1894449685;
        }
    }

    /**
     * New call was created or information about a call was updated.
     */
    public static class UpdateCall extends Update {
        /**
         * New data about a call.
         */
        public Call call;

        /**
         * Default constructor.
         */
        public UpdateCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param call New data about a call.
         */
        public UpdateCall(Call call) {
            this.call = call;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1337184477;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1337184477;
        }
    }

    /**
     * Some privacy setting rules have been changed.
     */
    public static class UpdateUserPrivacySettingRules extends Update {
        /**
         * The privacy setting.
         */
        public UserPrivacySetting setting;
        /**
         * New privacy rules.
         */
        public UserPrivacySettingRules rules;

        /**
         * Default constructor.
         */
        public UpdateUserPrivacySettingRules() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setting The privacy setting.
         * @param rules New privacy rules.
         */
        public UpdateUserPrivacySettingRules(UserPrivacySetting setting, UserPrivacySettingRules rules) {
            this.setting = setting;
            this.rules = rules;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -912960778;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -912960778;
        }
    }

    /**
     * Number of unread messages has changed. This update is sent only if a message database is used.
     */
    public static class UpdateUnreadMessageCount extends Update {
        /**
         * Total number of unread messages.
         */
        public int unreadCount;
        /**
         * Total number of unread messages in unmuted chats.
         */
        public int unreadUnmutedCount;

        /**
         * Default constructor.
         */
        public UpdateUnreadMessageCount() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param unreadCount Total number of unread messages.
         * @param unreadUnmutedCount Total number of unread messages in unmuted chats.
         */
        public UpdateUnreadMessageCount(int unreadCount, int unreadUnmutedCount) {
            this.unreadCount = unreadCount;
            this.unreadUnmutedCount = unreadUnmutedCount;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -824420376;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -824420376;
        }
    }

    /**
     * An option changed its value.
     */
    public static class UpdateOption extends Update {
        /**
         * The option name.
         */
        public String name;
        /**
         * The new option value.
         */
        public OptionValue value;

        /**
         * Default constructor.
         */
        public UpdateOption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name The option name.
         * @param value The new option value.
         */
        public UpdateOption(String name, OptionValue value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 900822020;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 900822020;
        }
    }

    /**
     * The list of installed sticker sets was updated.
     */
    public static class UpdateInstalledStickerSets extends Update {
        /**
         * True, if the list of installed mask sticker sets was updated.
         */
        public boolean isMasks;
        /**
         * The new list of installed ordinary sticker sets.
         */
        public long[] stickerSetIds;

        /**
         * Default constructor.
         */
        public UpdateInstalledStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMasks True, if the list of installed mask sticker sets was updated.
         * @param stickerSetIds The new list of installed ordinary sticker sets.
         */
        public UpdateInstalledStickerSets(boolean isMasks, long[] stickerSetIds) {
            this.isMasks = isMasks;
            this.stickerSetIds = stickerSetIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1125575977;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1125575977;
        }
    }

    /**
     * The list of trending sticker sets was updated or some of them were viewed.
     */
    public static class UpdateTrendingStickerSets extends Update {
        /**
         * The new list of trending sticker sets.
         */
        public StickerSets stickerSets;

        /**
         * Default constructor.
         */
        public UpdateTrendingStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param stickerSets The new list of trending sticker sets.
         */
        public UpdateTrendingStickerSets(StickerSets stickerSets) {
            this.stickerSets = stickerSets;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 450714593;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 450714593;
        }
    }

    /**
     * The list of recently used stickers was updated.
     */
    public static class UpdateRecentStickers extends Update {
        /**
         * True, if the list of stickers attached to photo or video files was updated, otherwise the list of sent stickers is updated.
         */
        public boolean isAttached;
        /**
         * The new list of file identifiers of recently used stickers.
         */
        public int[] stickerIds;

        /**
         * Default constructor.
         */
        public UpdateRecentStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAttached True, if the list of stickers attached to photo or video files was updated, otherwise the list of sent stickers is updated.
         * @param stickerIds The new list of file identifiers of recently used stickers.
         */
        public UpdateRecentStickers(boolean isAttached, int[] stickerIds) {
            this.isAttached = isAttached;
            this.stickerIds = stickerIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1906403540;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1906403540;
        }
    }

    /**
     * The list of favorite stickers was updated.
     */
    public static class UpdateFavoriteStickers extends Update {
        /**
         * The new list of file identifiers of favorite stickers.
         */
        public int[] stickerIds;

        /**
         * Default constructor.
         */
        public UpdateFavoriteStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param stickerIds The new list of file identifiers of favorite stickers.
         */
        public UpdateFavoriteStickers(int[] stickerIds) {
            this.stickerIds = stickerIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1662240999;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1662240999;
        }
    }

    /**
     * The list of saved animations was updated.
     */
    public static class UpdateSavedAnimations extends Update {
        /**
         * The new list of file identifiers of saved animations.
         */
        public int[] animationIds;

        /**
         * Default constructor.
         */
        public UpdateSavedAnimations() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animationIds The new list of file identifiers of saved animations.
         */
        public UpdateSavedAnimations(int[] animationIds) {
            this.animationIds = animationIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 65563814;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 65563814;
        }
    }

    /**
     * The connection state has changed.
     */
    public static class UpdateConnectionState extends Update {
        /**
         * The new connection state.
         */
        public ConnectionState state;

        /**
         * Default constructor.
         */
        public UpdateConnectionState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param state The new connection state.
         */
        public UpdateConnectionState(ConnectionState state) {
            this.state = state;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1469292078;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1469292078;
        }
    }

    /**
     * A new incoming inline query; for bots only.
     */
    public static class UpdateNewInlineQuery extends Update {
        /**
         * Unique query identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * User location, provided by the client; may be null.
         */
        public @Nullable Location userLocation;
        /**
         * Text of the query.
         */
        public String query;
        /**
         * Offset of the first entry to return.
         */
        public String offset;

        /**
         * Default constructor.
         */
        public UpdateNewInlineQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique query identifier.
         * @param senderUserId Identifier of the user who sent the query.
         * @param userLocation User location, provided by the client; may be null.
         * @param query Text of the query.
         * @param offset Offset of the first entry to return.
         */
        public UpdateNewInlineQuery(long id, int senderUserId, Location userLocation, String query, String offset) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.userLocation = userLocation;
            this.query = query;
            this.offset = offset;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2064730634;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2064730634;
        }
    }

    /**
     * The user has chosen a result of an inline query; for bots only.
     */
    public static class UpdateNewChosenInlineResult extends Update {
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * User location, provided by the client; may be null.
         */
        public @Nullable Location userLocation;
        /**
         * Text of the query.
         */
        public String query;
        /**
         * Identifier of the chosen result.
         */
        public String resultId;
        /**
         * Identifier of the sent inline message, if known.
         */
        public String inlineMessageId;

        /**
         * Default constructor.
         */
        public UpdateNewChosenInlineResult() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param senderUserId Identifier of the user who sent the query.
         * @param userLocation User location, provided by the client; may be null.
         * @param query Text of the query.
         * @param resultId Identifier of the chosen result.
         * @param inlineMessageId Identifier of the sent inline message, if known.
         */
        public UpdateNewChosenInlineResult(int senderUserId, Location userLocation, String query, String resultId, String inlineMessageId) {
            this.senderUserId = senderUserId;
            this.userLocation = userLocation;
            this.query = query;
            this.resultId = resultId;
            this.inlineMessageId = inlineMessageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 527526965;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 527526965;
        }
    }

    /**
     * A new incoming callback query; for bots only.
     */
    public static class UpdateNewCallbackQuery extends Update {
        /**
         * Unique query identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * Identifier of the chat, in which the query was sent.
         */
        public long chatId;
        /**
         * Identifier of the message, from which the query originated.
         */
        public long messageId;
        /**
         * Identifier that uniquely corresponds to the chat to which the message was sent.
         */
        public long chatInstance;
        /**
         * Query payload.
         */
        public CallbackQueryPayload payload;

        /**
         * Default constructor.
         */
        public UpdateNewCallbackQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique query identifier.
         * @param senderUserId Identifier of the user who sent the query.
         * @param chatId Identifier of the chat, in which the query was sent.
         * @param messageId Identifier of the message, from which the query originated.
         * @param chatInstance Identifier that uniquely corresponds to the chat to which the message was sent.
         * @param payload Query payload.
         */
        public UpdateNewCallbackQuery(long id, int senderUserId, long chatId, long messageId, long chatInstance, CallbackQueryPayload payload) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.chatId = chatId;
            this.messageId = messageId;
            this.chatInstance = chatInstance;
            this.payload = payload;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2044226370;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2044226370;
        }
    }

    /**
     * A new incoming callback query from a message sent via a bot; for bots only.
     */
    public static class UpdateNewInlineCallbackQuery extends Update {
        /**
         * Unique query identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * Identifier of the inline message, from which the query originated.
         */
        public String inlineMessageId;
        /**
         * An identifier uniquely corresponding to the chat a message was sent to.
         */
        public long chatInstance;
        /**
         * Query payload.
         */
        public CallbackQueryPayload payload;

        /**
         * Default constructor.
         */
        public UpdateNewInlineCallbackQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique query identifier.
         * @param senderUserId Identifier of the user who sent the query.
         * @param inlineMessageId Identifier of the inline message, from which the query originated.
         * @param chatInstance An identifier uniquely corresponding to the chat a message was sent to.
         * @param payload Query payload.
         */
        public UpdateNewInlineCallbackQuery(long id, int senderUserId, String inlineMessageId, long chatInstance, CallbackQueryPayload payload) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.inlineMessageId = inlineMessageId;
            this.chatInstance = chatInstance;
            this.payload = payload;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1879154829;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1879154829;
        }
    }

    /**
     * A new incoming shipping query; for bots only. Only for invoices with flexible price.
     */
    public static class UpdateNewShippingQuery extends Update {
        /**
         * Unique query identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * Invoice payload.
         */
        public String invoicePayload;
        /**
         * User shipping address.
         */
        public ShippingAddress shippingAddress;

        /**
         * Default constructor.
         */
        public UpdateNewShippingQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique query identifier.
         * @param senderUserId Identifier of the user who sent the query.
         * @param invoicePayload Invoice payload.
         * @param shippingAddress User shipping address.
         */
        public UpdateNewShippingQuery(long id, int senderUserId, String invoicePayload, ShippingAddress shippingAddress) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.invoicePayload = invoicePayload;
            this.shippingAddress = shippingAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1877838488;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1877838488;
        }
    }

    /**
     * A new incoming pre-checkout query; for bots only. Contains full information about a checkout.
     */
    public static class UpdateNewPreCheckoutQuery extends Update {
        /**
         * Unique query identifier.
         */
        public long id;
        /**
         * Identifier of the user who sent the query.
         */
        public int senderUserId;
        /**
         * Currency for the product price.
         */
        public String currency;
        /**
         * Total price for the product, in the minimal quantity of the currency.
         */
        public long totalAmount;
        /**
         * Invoice payload.
         */
        public byte[] invoicePayload;
        /**
         * Identifier of a shipping option chosen by the user; may be empty if not applicable.
         */
        public String shippingOptionId;
        /**
         * Information about the order; may be null.
         */
        public @Nullable OrderInfo orderInfo;

        /**
         * Default constructor.
         */
        public UpdateNewPreCheckoutQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique query identifier.
         * @param senderUserId Identifier of the user who sent the query.
         * @param currency Currency for the product price.
         * @param totalAmount Total price for the product, in the minimal quantity of the currency.
         * @param invoicePayload Invoice payload.
         * @param shippingOptionId Identifier of a shipping option chosen by the user; may be empty if not applicable.
         * @param orderInfo Information about the order; may be null.
         */
        public UpdateNewPreCheckoutQuery(long id, int senderUserId, String currency, long totalAmount, byte[] invoicePayload, String shippingOptionId, OrderInfo orderInfo) {
            this.id = id;
            this.senderUserId = senderUserId;
            this.currency = currency;
            this.totalAmount = totalAmount;
            this.invoicePayload = invoicePayload;
            this.shippingOptionId = shippingOptionId;
            this.orderInfo = orderInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 87964006;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 87964006;
        }
    }

    /**
     * A new incoming event; for bots only.
     */
    public static class UpdateNewCustomEvent extends Update {
        /**
         * A JSON-serialized event.
         */
        public String event;

        /**
         * Default constructor.
         */
        public UpdateNewCustomEvent() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param event A JSON-serialized event.
         */
        public UpdateNewCustomEvent(String event) {
            this.event = event;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1994222092;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1994222092;
        }
    }

    /**
     * A new incoming query; for bots only.
     */
    public static class UpdateNewCustomQuery extends Update {
        /**
         * The query identifier.
         */
        public long id;
        /**
         * JSON-serialized query data.
         */
        public String data;
        /**
         * Query timeout.
         */
        public int timeout;

        /**
         * Default constructor.
         */
        public UpdateNewCustomQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id The query identifier.
         * @param data JSON-serialized query data.
         * @param timeout Query timeout.
         */
        public UpdateNewCustomQuery(long id, String data, int timeout) {
            this.id = id;
            this.data = data;
            this.timeout = timeout;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -687670874;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -687670874;
        }
    }

    /**
     * Represents a user.
     */
    public static class User extends Object {
        /**
         * User identifier.
         */
        public int id;
        /**
         * First name of the user.
         */
        public String firstName;
        /**
         * Last name of the user.
         */
        public String lastName;
        /**
         * Username of the user.
         */
        public String username;
        /**
         * Phone number of the user.
         */
        public String phoneNumber;
        /**
         * Current online status of the user.
         */
        public UserStatus status;
        /**
         * Profile photo of the user; may be null.
         */
        public @Nullable ProfilePhoto profilePhoto;
        /**
         * Relationship from the current user to the other user.
         */
        public LinkState outgoingLink;
        /**
         * Relationship from the other user to the current user.
         */
        public LinkState incomingLink;
        /**
         * True, if the user is verified.
         */
        public boolean isVerified;
        /**
         * If non-empty, it contains the reason why access to this user must be restricted. The format of the string is &quot;{type}: {description}&quot;. {type} contains the type of the restriction and at least one of the suffixes &quot;-all&quot;, &quot;-ios&quot;, &quot;-android&quot;, or &quot;-wp&quot;, which describe the platforms on which access should be restricted. (For example, &quot;terms-ios-android&quot;. {description} contains a human-readable description of the restriction, which can be shown to the user.)
         */
        public String restrictionReason;
        /**
         * If false, the user is inaccessible, and the only information known about the user is inside this class. It can't be passed to any method except GetUser.
         */
        public boolean haveAccess;
        /**
         * Type of the user.
         */
        public UserType type;
        /**
         * IETF language tag of the user's language; only available to bots.
         */
        public String languageCode;

        /**
         * Default constructor.
         */
        public User() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id User identifier.
         * @param firstName First name of the user.
         * @param lastName Last name of the user.
         * @param username Username of the user.
         * @param phoneNumber Phone number of the user.
         * @param status Current online status of the user.
         * @param profilePhoto Profile photo of the user; may be null.
         * @param outgoingLink Relationship from the current user to the other user.
         * @param incomingLink Relationship from the other user to the current user.
         * @param isVerified True, if the user is verified.
         * @param restrictionReason If non-empty, it contains the reason why access to this user must be restricted. The format of the string is &quot;{type}: {description}&quot;. {type} contains the type of the restriction and at least one of the suffixes &quot;-all&quot;, &quot;-ios&quot;, &quot;-android&quot;, or &quot;-wp&quot;, which describe the platforms on which access should be restricted. (For example, &quot;terms-ios-android&quot;. {description} contains a human-readable description of the restriction, which can be shown to the user.)
         * @param haveAccess If false, the user is inaccessible, and the only information known about the user is inside this class. It can't be passed to any method except GetUser.
         * @param type Type of the user.
         * @param languageCode IETF language tag of the user's language; only available to bots.
         */
        public User(int id, String firstName, String lastName, String username, String phoneNumber, UserStatus status, ProfilePhoto profilePhoto, LinkState outgoingLink, LinkState incomingLink, boolean isVerified, String restrictionReason, boolean haveAccess, UserType type, String languageCode) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.phoneNumber = phoneNumber;
            this.status = status;
            this.profilePhoto = profilePhoto;
            this.outgoingLink = outgoingLink;
            this.incomingLink = incomingLink;
            this.isVerified = isVerified;
            this.restrictionReason = restrictionReason;
            this.haveAccess = haveAccess;
            this.type = type;
            this.languageCode = languageCode;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -732086407;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -732086407;
        }
    }

    /**
     * Contains full information about a user (except the full list of profile photos).
     */
    public static class UserFullInfo extends Object {
        /**
         * True, if the user is blacklisted by the current user.
         */
        public boolean isBlocked;
        /**
         * True, if the user can be called.
         */
        public boolean canBeCalled;
        /**
         * True, if the user can't be called due to their privacy settings.
         */
        public boolean hasPrivateCalls;
        /**
         * A short user bio.
         */
        public String bio;
        /**
         * For bots, the text that is included with the link when users share the bot.
         */
        public String shareText;
        /**
         * Number of group chats where both the other user and the current user are a member; 0 for the current user.
         */
        public int groupInCommonCount;
        /**
         * If the user is a bot, information about the bot; may be null.
         */
        public @Nullable BotInfo botInfo;

        /**
         * Default constructor.
         */
        public UserFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isBlocked True, if the user is blacklisted by the current user.
         * @param canBeCalled True, if the user can be called.
         * @param hasPrivateCalls True, if the user can't be called due to their privacy settings.
         * @param bio A short user bio.
         * @param shareText For bots, the text that is included with the link when users share the bot.
         * @param groupInCommonCount Number of group chats where both the other user and the current user are a member; 0 for the current user.
         * @param botInfo If the user is a bot, information about the bot; may be null.
         */
        public UserFullInfo(boolean isBlocked, boolean canBeCalled, boolean hasPrivateCalls, String bio, String shareText, int groupInCommonCount, BotInfo botInfo) {
            this.isBlocked = isBlocked;
            this.canBeCalled = canBeCalled;
            this.hasPrivateCalls = hasPrivateCalls;
            this.bio = bio;
            this.shareText = shareText;
            this.groupInCommonCount = groupInCommonCount;
            this.botInfo = botInfo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1076948004;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1076948004;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes available user privacy settings.
     */
    public abstract static class UserPrivacySetting extends Object {
    }

    /**
     * A privacy setting for managing whether the user's online status is visible.
     */
    public static class UserPrivacySettingShowStatus extends UserPrivacySetting {

        /**
         * Default constructor.
         */
        public UserPrivacySettingShowStatus() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1862829310;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1862829310;
        }
    }

    /**
     * A privacy setting for managing whether the user can be invited to chats.
     */
    public static class UserPrivacySettingAllowChatInvites extends UserPrivacySetting {

        /**
         * Default constructor.
         */
        public UserPrivacySettingAllowChatInvites() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1271668007;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1271668007;
        }
    }

    /**
     * A privacy setting for managing whether the user can be called.
     */
    public static class UserPrivacySettingAllowCalls extends UserPrivacySetting {

        /**
         * Default constructor.
         */
        public UserPrivacySettingAllowCalls() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -906967291;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -906967291;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents a single rule for managing privacy settings.
     */
    public abstract static class UserPrivacySettingRule extends Object {
    }

    /**
     * A rule to allow all users to do something.
     */
    public static class UserPrivacySettingRuleAllowAll extends UserPrivacySettingRule {

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleAllowAll() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1967186881;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1967186881;
        }
    }

    /**
     * A rule to allow all of a user's contacts to do something.
     */
    public static class UserPrivacySettingRuleAllowContacts extends UserPrivacySettingRule {

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleAllowContacts() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1892733680;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1892733680;
        }
    }

    /**
     * A rule to allow certain specified users to do something.
     */
    public static class UserPrivacySettingRuleAllowUsers extends UserPrivacySettingRule {
        /**
         * The user identifiers.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleAllowUsers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userIds The user identifiers.
         */
        public UserPrivacySettingRuleAllowUsers(int[] userIds) {
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 427601278;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 427601278;
        }
    }

    /**
     * A rule to restrict all users from doing something.
     */
    public static class UserPrivacySettingRuleRestrictAll extends UserPrivacySettingRule {

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleRestrictAll() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1406495408;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1406495408;
        }
    }

    /**
     * A rule to restrict all contacts of a user from doing something.
     */
    public static class UserPrivacySettingRuleRestrictContacts extends UserPrivacySettingRule {

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleRestrictContacts() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1008389378;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1008389378;
        }
    }

    /**
     * A rule to restrict all specified users from doing something.
     */
    public static class UserPrivacySettingRuleRestrictUsers extends UserPrivacySettingRule {
        /**
         * The user identifiers.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public UserPrivacySettingRuleRestrictUsers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userIds The user identifiers.
         */
        public UserPrivacySettingRuleRestrictUsers(int[] userIds) {
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2119951802;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2119951802;
        }
    }

    /**
     * A list of privacy rules. Rules are matched in the specified order. The first matched rule defines the privacy setting for a given user. If no rule matches, the action is not allowed.
     */
    public static class UserPrivacySettingRules extends Object {
        /**
         * A list of rules.
         */
        public UserPrivacySettingRule[] rules;

        /**
         * Default constructor.
         */
        public UserPrivacySettingRules() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param rules A list of rules.
         */
        public UserPrivacySettingRules(UserPrivacySettingRule[] rules) {
            this.rules = rules;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 322477541;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 322477541;
        }
    }

    /**
     * Contains part of the list of user photos.
     */
    public static class UserProfilePhotos extends Object {
        /**
         * Total number of user profile photos.
         */
        public int totalCount;
        /**
         * A list of photos.
         */
        public Photo[] photos;

        /**
         * Default constructor.
         */
        public UserProfilePhotos() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param totalCount Total number of user profile photos.
         * @param photos A list of photos.
         */
        public UserProfilePhotos(int totalCount, Photo[] photos) {
            this.totalCount = totalCount;
            this.photos = photos;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1388892074;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1388892074;
        }
    }

    /**
     * This class is an abstract base class.
     * Describes the last time the user was online.
     */
    public abstract static class UserStatus extends Object {
    }

    /**
     * The user status was never changed.
     */
    public static class UserStatusEmpty extends UserStatus {

        /**
         * Default constructor.
         */
        public UserStatusEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 164646985;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 164646985;
        }
    }

    /**
     * The user is online.
     */
    public static class UserStatusOnline extends UserStatus {
        /**
         * Point in time (Unix timestamp) when the user's online status will expire.
         */
        public int expires;

        /**
         * Default constructor.
         */
        public UserStatusOnline() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param expires Point in time (Unix timestamp) when the user's online status will expire.
         */
        public UserStatusOnline(int expires) {
            this.expires = expires;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1529460876;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1529460876;
        }
    }

    /**
     * The user is offline.
     */
    public static class UserStatusOffline extends UserStatus {
        /**
         * Point in time (Unix timestamp) when the user was last online.
         */
        public int wasOnline;

        /**
         * Default constructor.
         */
        public UserStatusOffline() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param wasOnline Point in time (Unix timestamp) when the user was last online.
         */
        public UserStatusOffline(int wasOnline) {
            this.wasOnline = wasOnline;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -759984891;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -759984891;
        }
    }

    /**
     * The user was online recently.
     */
    public static class UserStatusRecently extends UserStatus {

        /**
         * Default constructor.
         */
        public UserStatusRecently() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -496024847;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -496024847;
        }
    }

    /**
     * The user is offline, but was online last week.
     */
    public static class UserStatusLastWeek extends UserStatus {

        /**
         * Default constructor.
         */
        public UserStatusLastWeek() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 129960444;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 129960444;
        }
    }

    /**
     * The user is offline, but was online last month.
     */
    public static class UserStatusLastMonth extends UserStatus {

        /**
         * Default constructor.
         */
        public UserStatusLastMonth() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2011940674;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2011940674;
        }
    }

    /**
     * This class is an abstract base class.
     * Represents the type of the user. The following types are possible: regular users, deleted users and bots.
     */
    public abstract static class UserType extends Object {
    }

    /**
     * A regular user.
     */
    public static class UserTypeRegular extends UserType {

        /**
         * Default constructor.
         */
        public UserTypeRegular() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -598644325;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -598644325;
        }
    }

    /**
     * A deleted user or deleted bot. No information on the user besides the userId is available. It is not possible to perform any active actions on this type of user.
     */
    public static class UserTypeDeleted extends UserType {

        /**
         * Default constructor.
         */
        public UserTypeDeleted() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1807729372;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1807729372;
        }
    }

    /**
     * A bot (see https://core.telegram.org/bots).
     */
    public static class UserTypeBot extends UserType {
        /**
         * True, if the bot can be invited to basic group and supergroup chats.
         */
        public boolean canJoinGroups;
        /**
         * True, if the bot can read all messages in basic group or supergroup chats and not just those addressed to the bot. In private and channel chats a bot can always read all messages.
         */
        public boolean canReadAllGroupMessages;
        /**
         * True, if the bot supports inline queries.
         */
        public boolean isInline;
        /**
         * Placeholder for inline queries (displayed on the client input field).
         */
        public String inlineQueryPlaceholder;
        /**
         * True, if the location of the user should be sent with every inline query to this bot.
         */
        public boolean needLocation;

        /**
         * Default constructor.
         */
        public UserTypeBot() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param canJoinGroups True, if the bot can be invited to basic group and supergroup chats.
         * @param canReadAllGroupMessages True, if the bot can read all messages in basic group or supergroup chats and not just those addressed to the bot. In private and channel chats a bot can always read all messages.
         * @param isInline True, if the bot supports inline queries.
         * @param inlineQueryPlaceholder Placeholder for inline queries (displayed on the client input field).
         * @param needLocation True, if the location of the user should be sent with every inline query to this bot.
         */
        public UserTypeBot(boolean canJoinGroups, boolean canReadAllGroupMessages, boolean isInline, String inlineQueryPlaceholder, boolean needLocation) {
            this.canJoinGroups = canJoinGroups;
            this.canReadAllGroupMessages = canReadAllGroupMessages;
            this.isInline = isInline;
            this.inlineQueryPlaceholder = inlineQueryPlaceholder;
            this.needLocation = needLocation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1262387765;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1262387765;
        }
    }

    /**
     * No information on the user besides the userId is available, yet this user has not been deleted. This object is extremely rare and must be handled like a deleted user. It is not possible to perform any actions on users of this type.
     */
    public static class UserTypeUnknown extends UserType {

        /**
         * Default constructor.
         */
        public UserTypeUnknown() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -724541123;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -724541123;
        }
    }

    /**
     * Represents a list of users.
     */
    public static class Users extends Object {
        /**
         * Approximate total count of users found.
         */
        public int totalCount;
        /**
         * A list of user identifiers.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public Users() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param totalCount Approximate total count of users found.
         * @param userIds A list of user identifiers.
         */
        public Users(int totalCount, int[] userIds) {
            this.totalCount = totalCount;
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 273760088;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 273760088;
        }
    }

    /**
     * Contains a temporary identifier of validated order information, which is stored for one hour. Also contains the available shipping options.
     */
    public static class ValidatedOrderInfo extends Object {
        /**
         * Temporary identifier of the order information.
         */
        public String orderInfoId;
        /**
         * Available shipping options.
         */
        public ShippingOption[] shippingOptions;

        /**
         * Default constructor.
         */
        public ValidatedOrderInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param orderInfoId Temporary identifier of the order information.
         * @param shippingOptions Available shipping options.
         */
        public ValidatedOrderInfo(String orderInfoId, ShippingOption[] shippingOptions) {
            this.orderInfoId = orderInfoId;
            this.shippingOptions = shippingOptions;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1511451484;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1511451484;
        }
    }

    /**
     * Describes a venue.
     */
    public static class Venue extends Object {
        /**
         * Venue location; as defined by the sender.
         */
        public Location location;
        /**
         * Venue name; as defined by the sender.
         */
        public String title;
        /**
         * Venue address; as defined by the sender.
         */
        public String address;
        /**
         * Provider of the venue database; as defined by the sender. Currently only &quot;foursquare&quot; needs to be supported.
         */
        public String provider;
        /**
         * Identifier of the venue in the provider database; as defined by the sender.
         */
        public String id;

        /**
         * Default constructor.
         */
        public Venue() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param location Venue location; as defined by the sender.
         * @param title Venue name; as defined by the sender.
         * @param address Venue address; as defined by the sender.
         * @param provider Provider of the venue database; as defined by the sender. Currently only &quot;foursquare&quot; needs to be supported.
         * @param id Identifier of the venue in the provider database; as defined by the sender.
         */
        public Venue(Location location, String title, String address, String provider, String id) {
            this.location = location;
            this.title = title;
            this.address = address;
            this.provider = provider;
            this.id = id;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -674687867;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -674687867;
        }
    }

    /**
     * Describes a video file.
     */
    public static class Video extends Object {
        /**
         * Duration of the video, in seconds; as defined by the sender.
         */
        public int duration;
        /**
         * Video width; as defined by the sender.
         */
        public int width;
        /**
         * Video height; as defined by the sender.
         */
        public int height;
        /**
         * Original name of the file; as defined by the sender.
         */
        public String fileName;
        /**
         * MIME type of the file; as defined by the sender.
         */
        public String mimeType;
        /**
         * True, if stickers were added to the photo.
         */
        public boolean hasStickers;
        /**
         * True, if the video should be tried to be streamed.
         */
        public boolean supportsStreaming;
        /**
         * Video thumbnail; as defined by the sender; may be null.
         */
        public @Nullable PhotoSize thumbnail;
        /**
         * File containing the video.
         */
        public File video;

        /**
         * Default constructor.
         */
        public Video() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param duration Duration of the video, in seconds; as defined by the sender.
         * @param width Video width; as defined by the sender.
         * @param height Video height; as defined by the sender.
         * @param fileName Original name of the file; as defined by the sender.
         * @param mimeType MIME type of the file; as defined by the sender.
         * @param hasStickers True, if stickers were added to the photo.
         * @param supportsStreaming True, if the video should be tried to be streamed.
         * @param thumbnail Video thumbnail; as defined by the sender; may be null.
         * @param video File containing the video.
         */
        public Video(int duration, int width, int height, String fileName, String mimeType, boolean hasStickers, boolean supportsStreaming, PhotoSize thumbnail, File video) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.hasStickers = hasStickers;
            this.supportsStreaming = supportsStreaming;
            this.thumbnail = thumbnail;
            this.video = video;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -437410347;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -437410347;
        }
    }

    /**
     * Describes a video note. The video must be equal in width and height, cropped to a circle, and stored in MPEG4 format.
     */
    public static class VideoNote extends Object {
        /**
         * Duration of the video, in seconds; as defined by the sender.
         */
        public int duration;
        /**
         * Video width and height; as defined by the sender.
         */
        public int length;
        /**
         * Video thumbnail; as defined by the sender; may be null.
         */
        public @Nullable PhotoSize thumbnail;
        /**
         * File containing the video.
         */
        public File video;

        /**
         * Default constructor.
         */
        public VideoNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param duration Duration of the video, in seconds; as defined by the sender.
         * @param length Video width and height; as defined by the sender.
         * @param thumbnail Video thumbnail; as defined by the sender; may be null.
         * @param video File containing the video.
         */
        public VideoNote(int duration, int length, PhotoSize thumbnail, File video) {
            this.duration = duration;
            this.length = length;
            this.thumbnail = thumbnail;
            this.video = video;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1177396120;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1177396120;
        }
    }

    /**
     * Describes a voice note. The voice note must be encoded with the Opus codec, and stored inside an OGG container. Voice notes can have only a single audio channel.
     */
    public static class VoiceNote extends Object {
        /**
         * Duration of the voice note, in seconds; as defined by the sender.
         */
        public int duration;
        /**
         * A waveform representation of the voice note in 5-bit format.
         */
        public byte[] waveform;
        /**
         * MIME type of the file; as defined by the sender.
         */
        public String mimeType;
        /**
         * File containing the voice note.
         */
        public File voice;

        /**
         * Default constructor.
         */
        public VoiceNote() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param duration Duration of the voice note, in seconds; as defined by the sender.
         * @param waveform A waveform representation of the voice note in 5-bit format.
         * @param mimeType MIME type of the file; as defined by the sender.
         * @param voice File containing the voice note.
         */
        public VoiceNote(int duration, byte[] waveform, String mimeType, File voice) {
            this.duration = duration;
            this.waveform = waveform;
            this.mimeType = mimeType;
            this.voice = voice;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2066012058;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2066012058;
        }
    }

    /**
     * Contains information about a wallpaper.
     */
    public static class Wallpaper extends Object {
        /**
         * Unique persistent wallpaper identifier.
         */
        public int id;
        /**
         * Available variants of the wallpaper in different sizes. These photos can only be downloaded; they can't be sent in a message.
         */
        public PhotoSize[] sizes;
        /**
         * Main color of the wallpaper in RGB24 format; should be treated as background color if no photos are specified.
         */
        public int color;

        /**
         * Default constructor.
         */
        public Wallpaper() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param id Unique persistent wallpaper identifier.
         * @param sizes Available variants of the wallpaper in different sizes. These photos can only be downloaded; they can't be sent in a message.
         * @param color Main color of the wallpaper in RGB24 format; should be treated as background color if no photos are specified.
         */
        public Wallpaper(int id, PhotoSize[] sizes, int color) {
            this.id = id;
            this.sizes = sizes;
            this.color = color;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 282771691;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 282771691;
        }
    }

    /**
     * Contains a list of wallpapers.
     */
    public static class Wallpapers extends Object {
        /**
         * A list of wallpapers.
         */
        public Wallpaper[] wallpapers;

        /**
         * Default constructor.
         */
        public Wallpapers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param wallpapers A list of wallpapers.
         */
        public Wallpapers(Wallpaper[] wallpapers) {
            this.wallpapers = wallpapers;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 877926640;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 877926640;
        }
    }

    /**
     * Describes a web page preview.
     */
    public static class WebPage extends Object {
        /**
         * Original URL of the link.
         */
        public String url;
        /**
         * URL to display.
         */
        public String displayUrl;
        /**
         * Type of the web page. Can be: article, photo, audio, video, document, profile, app, or something else.
         */
        public String type;
        /**
         * Short name of the site (e.g., Google Docs, App Store).
         */
        public String siteName;
        /**
         * Title of the content.
         */
        public String title;
        /**
         * Description of the content.
         */
        public String description;
        /**
         * Image representing the content; may be null.
         */
        public @Nullable Photo photo;
        /**
         * URL to show in the embedded preview.
         */
        public String embedUrl;
        /**
         * MIME type of the embedded preview, (e.g., text/html or video/mp4).
         */
        public String embedType;
        /**
         * Width of the embedded preview.
         */
        public int embedWidth;
        /**
         * Height of the embedded preview.
         */
        public int embedHeight;
        /**
         * Duration of the content, in seconds.
         */
        public int duration;
        /**
         * Author of the content.
         */
        public String author;
        /**
         * Preview of the content as an animation, if available; may be null.
         */
        public @Nullable Animation animation;
        /**
         * Preview of the content as an audio file, if available; may be null.
         */
        public @Nullable Audio audio;
        /**
         * Preview of the content as a document, if available (currently only available for small PDF files and ZIP archives); may be null.
         */
        public @Nullable Document document;
        /**
         * Preview of the content as a sticker for small WEBP files, if available; may be null.
         */
        public @Nullable Sticker sticker;
        /**
         * Preview of the content as a video, if available; may be null.
         */
        public @Nullable Video video;
        /**
         * Preview of the content as a video note, if available; may be null.
         */
        public @Nullable VideoNote videoNote;
        /**
         * Preview of the content as a voice note, if available; may be null.
         */
        public @Nullable VoiceNote voiceNote;
        /**
         * True, if the web page has an instant view.
         */
        public boolean hasInstantView;

        /**
         * Default constructor.
         */
        public WebPage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url Original URL of the link.
         * @param displayUrl URL to display.
         * @param type Type of the web page. Can be: article, photo, audio, video, document, profile, app, or something else.
         * @param siteName Short name of the site (e.g., Google Docs, App Store).
         * @param title Title of the content.
         * @param description Description of the content.
         * @param photo Image representing the content; may be null.
         * @param embedUrl URL to show in the embedded preview.
         * @param embedType MIME type of the embedded preview, (e.g., text/html or video/mp4).
         * @param embedWidth Width of the embedded preview.
         * @param embedHeight Height of the embedded preview.
         * @param duration Duration of the content, in seconds.
         * @param author Author of the content.
         * @param animation Preview of the content as an animation, if available; may be null.
         * @param audio Preview of the content as an audio file, if available; may be null.
         * @param document Preview of the content as a document, if available (currently only available for small PDF files and ZIP archives); may be null.
         * @param sticker Preview of the content as a sticker for small WEBP files, if available; may be null.
         * @param video Preview of the content as a video, if available; may be null.
         * @param videoNote Preview of the content as a video note, if available; may be null.
         * @param voiceNote Preview of the content as a voice note, if available; may be null.
         * @param hasInstantView True, if the web page has an instant view.
         */
        public WebPage(String url, String displayUrl, String type, String siteName, String title, String description, Photo photo, String embedUrl, String embedType, int embedWidth, int embedHeight, int duration, String author, Animation animation, Audio audio, Document document, Sticker sticker, Video video, VideoNote videoNote, VoiceNote voiceNote, boolean hasInstantView) {
            this.url = url;
            this.displayUrl = displayUrl;
            this.type = type;
            this.siteName = siteName;
            this.title = title;
            this.description = description;
            this.photo = photo;
            this.embedUrl = embedUrl;
            this.embedType = embedType;
            this.embedWidth = embedWidth;
            this.embedHeight = embedHeight;
            this.duration = duration;
            this.author = author;
            this.animation = animation;
            this.audio = audio;
            this.document = document;
            this.sticker = sticker;
            this.video = video;
            this.videoNote = videoNote;
            this.voiceNote = voiceNote;
            this.hasInstantView = hasInstantView;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1465949075;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1465949075;
        }
    }

    /**
     * Describes an instant view page for a web page.
     */
    public static class WebPageInstantView extends Object {
        /**
         * Content of the web page.
         */
        public PageBlock[] pageBlocks;
        /**
         * True, if the instant view contains the full page. A network request might be needed to get the full web page instant view.
         */
        public boolean isFull;

        /**
         * Default constructor.
         */
        public WebPageInstantView() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pageBlocks Content of the web page.
         * @param isFull True, if the instant view contains the full page. A network request might be needed to get the full web page instant view.
         */
        public WebPageInstantView(PageBlock[] pageBlocks, boolean isFull) {
            this.pageBlocks = pageBlocks;
            this.isFull = isFull;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1804324850;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1804324850;
        }
    }

    /**
     * Accepts an incoming call.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AcceptCall extends Function {
        /**
         * Call identifier.
         */
        public int callId;
        /**
         * Description of the call protocols supported by the client.
         */
        public CallProtocol protocol;

        /**
         * Default constructor.
         */
        public AcceptCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param callId Call identifier.
         * @param protocol Description of the call protocols supported by the client.
         */
        public AcceptCall(int callId, CallProtocol protocol) {
            this.callId = callId;
            this.protocol = protocol;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -646618416;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -646618416;
        }
    }

    /**
     * Adds a new member to a chat. Members can't be added to private or secret chats. Members will not be added until the chat state has been synchronized with the server.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddChatMember extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of the user.
         */
        public int userId;
        /**
         * The number of earlier messages from the chat to be forwarded to the new member; up to 300. Ignored for supergroups and channels.
         */
        public int forwardLimit;

        /**
         * Default constructor.
         */
        public AddChatMember() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userId Identifier of the user.
         * @param forwardLimit The number of earlier messages from the chat to be forwarded to the new member; up to 300. Ignored for supergroups and channels.
         */
        public AddChatMember(long chatId, int userId, int forwardLimit) {
            this.chatId = chatId;
            this.userId = userId;
            this.forwardLimit = forwardLimit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1182817962;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1182817962;
        }
    }

    /**
     * Adds multiple new members to a chat. Currently this option is only available for supergroups and channels. This option can't be used to join a chat. Members can't be added to a channel if it has more than 200 members. Members will not be added until the chat state has been synchronized with the server.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddChatMembers extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifiers of the users to be added to the chat.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public AddChatMembers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userIds Identifiers of the users to be added to the chat.
         */
        public AddChatMembers(long chatId, int[] userIds) {
            this.chatId = chatId;
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1234094617;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1234094617;
        }
    }

    /**
     * Adds a new sticker to the list of favorite stickers. The new sticker is added to the top of the list. If the sticker was already in the list, it is removed from the list first. Only stickers belonging to a sticker set can be added to this list.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddFavoriteSticker extends Function {
        /**
         * Sticker file to add.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public AddFavoriteSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker file to add.
         */
        public AddFavoriteSticker(InputFile sticker) {
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 324504799;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 324504799;
        }
    }

    /**
     * Adds the specified data to data usage statistics. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddNetworkStatistics extends Function {
        /**
         * The network statistics entry with the data to be added to statistics.
         */
        public NetworkStatisticsEntry entry;

        /**
         * Default constructor.
         */
        public AddNetworkStatistics() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param entry The network statistics entry with the data to be added to statistics.
         */
        public AddNetworkStatistics(NetworkStatisticsEntry entry) {
            this.entry = entry;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1264825305;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1264825305;
        }
    }

    /**
     * Manually adds a new sticker to the list of recently used stickers. The new sticker is added to the top of the list. If the sticker was already in the list, it is removed from the list first. Only stickers belonging to a sticker set can be added to this list.
     *
     * <p> Returns {@link Stickers Stickers} </p>
     */
    public static class AddRecentSticker extends Function {
        /**
         * Pass true to add the sticker to the list of stickers recently attached to photo or video files; pass false to add the sticker to the list of recently sent stickers.
         */
        public boolean isAttached;
        /**
         * Sticker file to add.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public AddRecentSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAttached Pass true to add the sticker to the list of stickers recently attached to photo or video files; pass false to add the sticker to the list of recently sent stickers.
         * @param sticker Sticker file to add.
         */
        public AddRecentSticker(boolean isAttached, InputFile sticker) {
            this.isAttached = isAttached;
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1478109026;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1478109026;
        }
    }

    /**
     * Adds a chat to the list of recently found chats. The chat is added to the beginning of the list. If the chat is already in the list, it will be removed from the list first.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddRecentlyFoundChat extends Function {
        /**
         * Identifier of the chat to add.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public AddRecentlyFoundChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat to add.
         */
        public AddRecentlyFoundChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1746396787;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1746396787;
        }
    }

    /**
     * Manually adds a new animation to the list of saved animations. The new animation is added to the beginning of the list. If the animation was already in the list, it is removed first. Only non-secret video animations with MIME type &quot;video/mp4&quot; can be added to the list.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AddSavedAnimation extends Function {
        /**
         * The animation file to be added. Only animations known to the server (i.e. successfully sent via a message) can be added to the list.
         */
        public InputFile animation;

        /**
         * Default constructor.
         */
        public AddSavedAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animation The animation file to be added. Only animations known to the server (i.e. successfully sent via a message) can be added to the list.
         */
        public AddSavedAnimation(InputFile animation) {
            this.animation = animation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1538525088;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1538525088;
        }
    }

    /**
     * Adds a new sticker to a set; for bots only. Returns the sticker set.
     *
     * <p> Returns {@link StickerSet StickerSet} </p>
     */
    public static class AddStickerToSet extends Function {
        /**
         * Sticker set owner.
         */
        public int userId;
        /**
         * Sticker set name.
         */
        public String name;
        /**
         * Sticker to add to the set.
         */
        public InputSticker sticker;

        /**
         * Default constructor.
         */
        public AddStickerToSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Sticker set owner.
         * @param name Sticker set name.
         * @param sticker Sticker to add to the set.
         */
        public AddStickerToSet(int userId, String name, InputSticker sticker) {
            this.userId = userId;
            this.name = name;
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1422402800;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1422402800;
        }
    }

    /**
     * Sets the result of a callback query; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AnswerCallbackQuery extends Function {
        /**
         * Identifier of the callback query.
         */
        public long callbackQueryId;
        /**
         * Text of the answer.
         */
        public String text;
        /**
         * If true, an alert should be shown to the user instead of a toast notification.
         */
        public boolean showAlert;
        /**
         * URL to be opened.
         */
        public String url;
        /**
         * Time during which the result of the query can be cached, in seconds.
         */
        public int cacheTime;

        /**
         * Default constructor.
         */
        public AnswerCallbackQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param callbackQueryId Identifier of the callback query.
         * @param text Text of the answer.
         * @param showAlert If true, an alert should be shown to the user instead of a toast notification.
         * @param url URL to be opened.
         * @param cacheTime Time during which the result of the query can be cached, in seconds.
         */
        public AnswerCallbackQuery(long callbackQueryId, String text, boolean showAlert, String url, int cacheTime) {
            this.callbackQueryId = callbackQueryId;
            this.text = text;
            this.showAlert = showAlert;
            this.url = url;
            this.cacheTime = cacheTime;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1153028490;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1153028490;
        }
    }

    /**
     * Answers a custom query; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AnswerCustomQuery extends Function {
        /**
         * Identifier of a custom query.
         */
        public long customQueryId;
        /**
         * JSON-serialized answer to the query.
         */
        public String data;

        /**
         * Default constructor.
         */
        public AnswerCustomQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param customQueryId Identifier of a custom query.
         * @param data JSON-serialized answer to the query.
         */
        public AnswerCustomQuery(long customQueryId, String data) {
            this.customQueryId = customQueryId;
            this.data = data;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1293603521;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1293603521;
        }
    }

    /**
     * Sets the result of an inline query; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AnswerInlineQuery extends Function {
        /**
         * Identifier of the inline query.
         */
        public long inlineQueryId;
        /**
         * True, if the result of the query can be cached for the specified user.
         */
        public boolean isPersonal;
        /**
         * The results of the query.
         */
        public InputInlineQueryResult[] results;
        /**
         * Allowed time to cache the results of the query, in seconds.
         */
        public int cacheTime;
        /**
         * Offset for the next inline query; pass an empty string if there are no more results.
         */
        public String nextOffset;
        /**
         * If non-empty, this text should be shown on the button that opens a private chat with the bot and sends a start message to the bot with the parameter switchPmParameter.
         */
        public String switchPmText;
        /**
         * The parameter for the bot start message.
         */
        public String switchPmParameter;

        /**
         * Default constructor.
         */
        public AnswerInlineQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineQueryId Identifier of the inline query.
         * @param isPersonal True, if the result of the query can be cached for the specified user.
         * @param results The results of the query.
         * @param cacheTime Allowed time to cache the results of the query, in seconds.
         * @param nextOffset Offset for the next inline query; pass an empty string if there are no more results.
         * @param switchPmText If non-empty, this text should be shown on the button that opens a private chat with the bot and sends a start message to the bot with the parameter switchPmParameter.
         * @param switchPmParameter The parameter for the bot start message.
         */
        public AnswerInlineQuery(long inlineQueryId, boolean isPersonal, InputInlineQueryResult[] results, int cacheTime, String nextOffset, String switchPmText, String switchPmParameter) {
            this.inlineQueryId = inlineQueryId;
            this.isPersonal = isPersonal;
            this.results = results;
            this.cacheTime = cacheTime;
            this.nextOffset = nextOffset;
            this.switchPmText = switchPmText;
            this.switchPmParameter = switchPmParameter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 485879477;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 485879477;
        }
    }

    /**
     * Sets the result of a pre-checkout query; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AnswerPreCheckoutQuery extends Function {
        /**
         * Identifier of the pre-checkout query.
         */
        public long preCheckoutQueryId;
        /**
         * An error message, empty on success.
         */
        public String errorMessage;

        /**
         * Default constructor.
         */
        public AnswerPreCheckoutQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param preCheckoutQueryId Identifier of the pre-checkout query.
         * @param errorMessage An error message, empty on success.
         */
        public AnswerPreCheckoutQuery(long preCheckoutQueryId, String errorMessage) {
            this.preCheckoutQueryId = preCheckoutQueryId;
            this.errorMessage = errorMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1486789653;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1486789653;
        }
    }

    /**
     * Sets the result of a shipping query; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class AnswerShippingQuery extends Function {
        /**
         * Identifier of the shipping query.
         */
        public long shippingQueryId;
        /**
         * Available shipping options.
         */
        public ShippingOption[] shippingOptions;
        /**
         * An error message, empty on success.
         */
        public String errorMessage;

        /**
         * Default constructor.
         */
        public AnswerShippingQuery() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param shippingQueryId Identifier of the shipping query.
         * @param shippingOptions Available shipping options.
         * @param errorMessage An error message, empty on success.
         */
        public AnswerShippingQuery(long shippingQueryId, ShippingOption[] shippingOptions, String errorMessage) {
            this.shippingQueryId = shippingQueryId;
            this.shippingOptions = shippingOptions;
            this.errorMessage = errorMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -434601324;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -434601324;
        }
    }

    /**
     * Adds a user to the blacklist.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class BlockUser extends Function {
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public BlockUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         */
        public BlockUser(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1239315139;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1239315139;
        }
    }

    /**
     * Stops the downloading of a file. If a file has already been downloaded, does nothing.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CancelDownloadFile extends Function {
        /**
         * Identifier of a file to stop downloading.
         */
        public int fileId;
        /**
         * Pass true to stop downloading only if it hasn't been started, i.e. request hasn't been sent to server.
         */
        public boolean onlyIfPending;

        /**
         * Default constructor.
         */
        public CancelDownloadFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId Identifier of a file to stop downloading.
         * @param onlyIfPending Pass true to stop downloading only if it hasn't been started, i.e. request hasn't been sent to server.
         */
        public CancelDownloadFile(int fileId, boolean onlyIfPending) {
            this.fileId = fileId;
            this.onlyIfPending = onlyIfPending;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1954524450;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1954524450;
        }
    }

    /**
     * Stops the uploading of a file. Supported only for files uploaded by using uploadFile. For other files the behavior is undefined.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CancelUploadFile extends Function {
        /**
         * Identifier of the file to stop uploading.
         */
        public int fileId;

        /**
         * Default constructor.
         */
        public CancelUploadFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId Identifier of the file to stop uploading.
         */
        public CancelUploadFile(int fileId) {
            this.fileId = fileId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1623539600;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1623539600;
        }
    }

    /**
     * Used to let the server know whether a chat is spam or not. Can be used only if ChatReportSpamState.canReportSpam is true. After this request, ChatReportSpamState.canReportSpam becomes false forever.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ChangeChatReportSpamState extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * If true, the chat will be reported as spam; otherwise it will be marked as not spam.
         */
        public boolean isSpamChat;

        /**
         * Default constructor.
         */
        public ChangeChatReportSpamState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param isSpamChat If true, the chat will be reported as spam; otherwise it will be marked as not spam.
         */
        public ChangeChatReportSpamState(long chatId, boolean isSpamChat) {
            this.chatId = chatId;
            this.isSpamChat = isSpamChat;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1768597097;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1768597097;
        }
    }

    /**
     * Changes imported contacts using the list of current user contacts saved on the device. Imports newly added contacts and, if at least the file database is enabled, deletes recently deleted contacts. Query result depends on the result of the previous query, so only one query is possible at the same time.
     *
     * <p> Returns {@link ImportedContacts ImportedContacts} </p>
     */
    public static class ChangeImportedContacts extends Function {
        /**
         * The new list of contacts.
         */
        public Contact[] contacts;

        /**
         * Default constructor.
         */
        public ChangeImportedContacts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param contacts The new list of contacts.
         */
        public ChangeImportedContacts(Contact[] contacts) {
            this.contacts = contacts;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1968207955;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1968207955;
        }
    }

    /**
     * Changes the phone number of the user and sends an authentication code to the user's new phone number. On success, returns information about the sent code.
     *
     * <p> Returns {@link AuthenticationCodeInfo AuthenticationCodeInfo} </p>
     */
    public static class ChangePhoneNumber extends Function {
        /**
         * The new phone number of the user in international format.
         */
        public String phoneNumber;
        /**
         * Pass true if the code can be sent via flash call to the specified phone number.
         */
        public boolean allowFlashCall;
        /**
         * Pass true if the phone number is used on the current device. Ignored if allowFlashCall is false.
         */
        public boolean isCurrentPhoneNumber;

        /**
         * Default constructor.
         */
        public ChangePhoneNumber() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param phoneNumber The new phone number of the user in international format.
         * @param allowFlashCall Pass true if the code can be sent via flash call to the specified phone number.
         * @param isCurrentPhoneNumber Pass true if the phone number is used on the current device. Ignored if allowFlashCall is false.
         */
        public ChangePhoneNumber(String phoneNumber, boolean allowFlashCall, boolean isCurrentPhoneNumber) {
            this.phoneNumber = phoneNumber;
            this.allowFlashCall = allowFlashCall;
            this.isCurrentPhoneNumber = isCurrentPhoneNumber;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1510625218;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1510625218;
        }
    }

    /**
     * Installs/uninstalls or activates/archives a sticker set.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ChangeStickerSet extends Function {
        /**
         * Identifier of the sticker set.
         */
        public long setId;
        /**
         * The new value of isInstalled.
         */
        public boolean isInstalled;
        /**
         * The new value of isArchived. A sticker set can't be installed and archived simultaneously.
         */
        public boolean isArchived;

        /**
         * Default constructor.
         */
        public ChangeStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setId Identifier of the sticker set.
         * @param isInstalled The new value of isInstalled.
         * @param isArchived The new value of isArchived. A sticker set can't be installed and archived simultaneously.
         */
        public ChangeStickerSet(long setId, boolean isInstalled, boolean isArchived) {
            this.setId = setId;
            this.isInstalled = isInstalled;
            this.isArchived = isArchived;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 449357293;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 449357293;
        }
    }

    /**
     * Checks the authentication token of a bot; to log in as a bot. Works only when the current authorization state is authorizationStateWaitPhoneNumber. Can be used instead of setAuthenticationPhoneNumber and checkAuthenticationCode to log in.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CheckAuthenticationBotToken extends Function {
        /**
         * The bot token.
         */
        public String token;

        /**
         * Default constructor.
         */
        public CheckAuthenticationBotToken() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param token The bot token.
         */
        public CheckAuthenticationBotToken(String token) {
            this.token = token;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 639321206;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 639321206;
        }
    }

    /**
     * Checks the authentication code. Works only when the current authorization state is authorizationStateWaitCode.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CheckAuthenticationCode extends Function {
        /**
         * The verification code received via SMS, Telegram message, phone call, or flash call.
         */
        public String code;
        /**
         * If the user is not yet registered, the first name of the user; 1-255 characters.
         */
        public String firstName;
        /**
         * If the user is not yet registered; the last name of the user; optional; 0-255 characters.
         */
        public String lastName;

        /**
         * Default constructor.
         */
        public CheckAuthenticationCode() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param code The verification code received via SMS, Telegram message, phone call, or flash call.
         * @param firstName If the user is not yet registered, the first name of the user; 1-255 characters.
         * @param lastName If the user is not yet registered; the last name of the user; optional; 0-255 characters.
         */
        public CheckAuthenticationCode(String code, String firstName, String lastName) {
            this.code = code;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -707293555;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -707293555;
        }
    }

    /**
     * Checks the authentication password for correctness. Works only when the current authorization state is authorizationStateWaitPassword.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CheckAuthenticationPassword extends Function {
        /**
         * The password to check.
         */
        public String password;

        /**
         * Default constructor.
         */
        public CheckAuthenticationPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param password The password to check.
         */
        public CheckAuthenticationPassword(String password) {
            this.password = password;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2025698400;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2025698400;
        }
    }

    /**
     * Checks the authentication code sent to confirm a new phone number of the user.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CheckChangePhoneNumberCode extends Function {
        /**
         * Verification code received by SMS, phone call or flash call.
         */
        public String code;

        /**
         * Default constructor.
         */
        public CheckChangePhoneNumberCode() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param code Verification code received by SMS, phone call or flash call.
         */
        public CheckChangePhoneNumberCode(String code) {
            this.code = code;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1720278429;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1720278429;
        }
    }

    /**
     * Checks the validity of an invite link for a chat and returns information about the corresponding chat.
     *
     * <p> Returns {@link ChatInviteLinkInfo ChatInviteLinkInfo} </p>
     */
    public static class CheckChatInviteLink extends Function {
        /**
         * Invite link to be checked; should begin with &quot;https://t.me/joinchat/&quot;, &quot;https://telegram.me/joinchat/&quot;, or &quot;https://telegram.dog/joinchat/&quot;.
         */
        public String inviteLink;

        /**
         * Default constructor.
         */
        public CheckChatInviteLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inviteLink Invite link to be checked; should begin with &quot;https://t.me/joinchat/&quot;, &quot;https://telegram.me/joinchat/&quot;, or &quot;https://telegram.dog/joinchat/&quot;.
         */
        public CheckChatInviteLink(String inviteLink) {
            this.inviteLink = inviteLink;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -496940997;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -496940997;
        }
    }

    /**
     * Checks whether a username can be set for a chat.
     *
     * <p> Returns {@link CheckChatUsernameResult CheckChatUsernameResult} </p>
     */
    public static class CheckChatUsername extends Function {
        /**
         * Chat identifier; should be identifier of a supergroup chat, or a channel chat, or a private chat with self, or zero if chat is being created.
         */
        public long chatId;
        /**
         * Username to be checked.
         */
        public String username;

        /**
         * Default constructor.
         */
        public CheckChatUsername() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier; should be identifier of a supergroup chat, or a channel chat, or a private chat with self, or zero if chat is being created.
         * @param username Username to be checked.
         */
        public CheckChatUsername(long chatId, String username) {
            this.chatId = chatId;
            this.username = username;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2003506154;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2003506154;
        }
    }

    /**
     * Checks the database encryption key for correctness. Works only when the current authorization state is authorizationStateWaitEncryptionKey.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CheckDatabaseEncryptionKey extends Function {
        /**
         * Encryption key to check or set up.
         */
        public byte[] encryptionKey;

        /**
         * Default constructor.
         */
        public CheckDatabaseEncryptionKey() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param encryptionKey Encryption key to check or set up.
         */
        public CheckDatabaseEncryptionKey(byte[] encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1018769307;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1018769307;
        }
    }

    /**
     * Clears all imported contacts.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ClearImportedContacts extends Function {

        /**
         * Default constructor.
         */
        public ClearImportedContacts() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 869503298;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 869503298;
        }
    }

    /**
     * Clears the list of recently used stickers.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ClearRecentStickers extends Function {
        /**
         * Pass true to clear the list of stickers recently attached to photo or video files; pass false to clear the list of recently sent stickers.
         */
        public boolean isAttached;

        /**
         * Default constructor.
         */
        public ClearRecentStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAttached Pass true to clear the list of stickers recently attached to photo or video files; pass false to clear the list of recently sent stickers.
         */
        public ClearRecentStickers(boolean isAttached) {
            this.isAttached = isAttached;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -321242684;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -321242684;
        }
    }

    /**
     * Clears the list of recently found chats.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ClearRecentlyFoundChats extends Function {

        /**
         * Default constructor.
         */
        public ClearRecentlyFoundChats() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -285582542;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -285582542;
        }
    }

    /**
     * Closes the TDLib instance. All databases will be flushed to disk and properly closed. After the close completes, updateAuthorizationState with authorizationStateClosed will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class Close extends Function {

        /**
         * Default constructor.
         */
        public Close() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1187782273;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1187782273;
        }
    }

    /**
     * This method should be called if the chat is closed by the user. Many useful activities depend on the chat being opened or closed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CloseChat extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public CloseChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public CloseChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 39749353;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 39749353;
        }
    }

    /**
     * Closes a secret chat, effectively transfering its state to secretChatStateClosed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class CloseSecretChat extends Function {
        /**
         * Secret chat identifier.
         */
        public int secretChatId;

        /**
         * Default constructor.
         */
        public CloseSecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param secretChatId Secret chat identifier.
         */
        public CloseSecretChat(int secretChatId) {
            this.secretChatId = secretChatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -471006133;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -471006133;
        }
    }

    /**
     * Returns an existing chat corresponding to a known basic group.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateBasicGroupChat extends Function {
        /**
         * Basic group identifier.
         */
        public int basicGroupId;
        /**
         * If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public boolean force;

        /**
         * Default constructor.
         */
        public CreateBasicGroupChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Basic group identifier.
         * @param force If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public CreateBasicGroupChat(int basicGroupId, boolean force) {
            this.basicGroupId = basicGroupId;
            this.force = force;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 642492777;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 642492777;
        }
    }

    /**
     * Creates a new call.
     *
     * <p> Returns {@link CallId CallId} </p>
     */
    public static class CreateCall extends Function {
        /**
         * Identifier of the user to be called.
         */
        public int userId;
        /**
         * Description of the call protocols supported by the client.
         */
        public CallProtocol protocol;

        /**
         * Default constructor.
         */
        public CreateCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Identifier of the user to be called.
         * @param protocol Description of the call protocols supported by the client.
         */
        public CreateCall(int userId, CallProtocol protocol) {
            this.userId = userId;
            this.protocol = protocol;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1742408159;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1742408159;
        }
    }

    /**
     * Creates a new basic group and sends a corresponding messageBasicGroupChatCreate. Returns the newly created chat.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateNewBasicGroupChat extends Function {
        /**
         * Identifiers of users to be added to the basic group.
         */
        public int[] userIds;
        /**
         * Title of the new basic group; 1-255 characters.
         */
        public String title;

        /**
         * Default constructor.
         */
        public CreateNewBasicGroupChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userIds Identifiers of users to be added to the basic group.
         * @param title Title of the new basic group; 1-255 characters.
         */
        public CreateNewBasicGroupChat(int[] userIds, String title) {
            this.userIds = userIds;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1874532069;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1874532069;
        }
    }

    /**
     * Creates a new secret chat. Returns the newly created chat.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateNewSecretChat extends Function {
        /**
         * Identifier of the target user.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public CreateNewSecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Identifier of the target user.
         */
        public CreateNewSecretChat(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1689344881;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1689344881;
        }
    }

    /**
     * Creates a new sticker set; for bots only. Returns the newly created sticker set.
     *
     * <p> Returns {@link StickerSet StickerSet} </p>
     */
    public static class CreateNewStickerSet extends Function {
        /**
         * Sticker set owner.
         */
        public int userId;
        /**
         * Sticker set title; 1-64 characters.
         */
        public String title;
        /**
         * Sticker set name. Can contain only English letters, digits and underscores. Must end with *&quot;By_&lt;bot username&gt;&quot;* (*&lt;botUsername&gt;* is case insensitive); 1-64 characters.
         */
        public String name;
        /**
         * True, if stickers are masks.
         */
        public boolean isMasks;
        /**
         * List of stickers to be added to the set.
         */
        public InputSticker[] stickers;

        /**
         * Default constructor.
         */
        public CreateNewStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Sticker set owner.
         * @param title Sticker set title; 1-64 characters.
         * @param name Sticker set name. Can contain only English letters, digits and underscores. Must end with *&quot;By_&lt;bot username&gt;&quot;* (*&lt;botUsername&gt;* is case insensitive); 1-64 characters.
         * @param isMasks True, if stickers are masks.
         * @param stickers List of stickers to be added to the set.
         */
        public CreateNewStickerSet(int userId, String title, String name, boolean isMasks, InputSticker[] stickers) {
            this.userId = userId;
            this.title = title;
            this.name = name;
            this.isMasks = isMasks;
            this.stickers = stickers;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 205093058;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 205093058;
        }
    }

    /**
     * Creates a new supergroup or channel and sends a corresponding messageSupergroupChatCreate. Returns the newly created chat.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateNewSupergroupChat extends Function {
        /**
         * Title of the new chat; 1-255 characters.
         */
        public String title;
        /**
         * True, if a channel chat should be created.
         */
        public boolean isChannel;
        /**
         * Chat description; 0-255 characters.
         */
        public String description;

        /**
         * Default constructor.
         */
        public CreateNewSupergroupChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param title Title of the new chat; 1-255 characters.
         * @param isChannel True, if a channel chat should be created.
         * @param description Chat description; 0-255 characters.
         */
        public CreateNewSupergroupChat(String title, boolean isChannel, String description) {
            this.title = title;
            this.isChannel = isChannel;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1284982268;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1284982268;
        }
    }

    /**
     * Returns an existing chat corresponding to a given user.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreatePrivateChat extends Function {
        /**
         * User identifier.
         */
        public int userId;
        /**
         * If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public boolean force;

        /**
         * Default constructor.
         */
        public CreatePrivateChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         * @param force If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public CreatePrivateChat(int userId, boolean force) {
            this.userId = userId;
            this.force = force;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1807530364;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1807530364;
        }
    }

    /**
     * Returns an existing chat corresponding to a known secret chat.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateSecretChat extends Function {
        /**
         * Secret chat identifier.
         */
        public int secretChatId;

        /**
         * Default constructor.
         */
        public CreateSecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param secretChatId Secret chat identifier.
         */
        public CreateSecretChat(int secretChatId) {
            this.secretChatId = secretChatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1930285615;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1930285615;
        }
    }

    /**
     * Returns an existing chat corresponding to a known supergroup or channel.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class CreateSupergroupChat extends Function {
        /**
         * Supergroup or channel identifier.
         */
        public int supergroupId;
        /**
         * If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public boolean force;

        /**
         * Default constructor.
         */
        public CreateSupergroupChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Supergroup or channel identifier.
         * @param force If true, the chat will be created without network request. In this case all information about the chat except its type, title and photo can be incorrect.
         */
        public CreateSupergroupChat(int supergroupId, boolean force) {
            this.supergroupId = supergroupId;
            this.force = force;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 352742758;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 352742758;
        }
    }

    /**
     * Creates a new temporary password for processing payments.
     *
     * <p> Returns {@link TemporaryPasswordState TemporaryPasswordState} </p>
     */
    public static class CreateTemporaryPassword extends Function {
        /**
         * Persistent user password.
         */
        public String password;
        /**
         * Time during which the temporary password will be valid, in seconds; should be between 60 and 86400.
         */
        public int validFor;

        /**
         * Default constructor.
         */
        public CreateTemporaryPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param password Persistent user password.
         * @param validFor Time during which the temporary password will be valid, in seconds; should be between 60 and 86400.
         */
        public CreateTemporaryPassword(String password, int validFor) {
            this.password = password;
            this.validFor = validFor;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1626509434;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1626509434;
        }
    }

    /**
     * Deletes the account of the current user, deleting all information associated with the user from the server. The phone number of the account can be used to create a new account.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteAccount extends Function {
        /**
         * The reason why the account was deleted; optional.
         */
        public String reason;

        /**
         * Default constructor.
         */
        public DeleteAccount() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param reason The reason why the account was deleted; optional.
         */
        public DeleteAccount(String reason) {
            this.reason = reason;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1203056508;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1203056508;
        }
    }

    /**
     * Deletes all messages in the chat only for the user. Cannot be used in channels and public supergroups.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteChatHistory extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Pass true if the chat should be removed from the chats list.
         */
        public boolean removeFromChatList;

        /**
         * Default constructor.
         */
        public DeleteChatHistory() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param removeFromChatList Pass true if the chat should be removed from the chats list.
         */
        public DeleteChatHistory(long chatId, boolean removeFromChatList) {
            this.chatId = chatId;
            this.removeFromChatList = removeFromChatList;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1384632722;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1384632722;
        }
    }

    /**
     * Deletes all messages sent by the specified user to a chat. Supported only in supergroups; requires canDeleteMessages administrator privileges.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteChatMessagesFromUser extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public DeleteChatMessagesFromUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userId User identifier.
         */
        public DeleteChatMessagesFromUser(long chatId, int userId) {
            this.chatId = chatId;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1599689199;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1599689199;
        }
    }

    /**
     * Deletes the default reply markup from a chat. Must be called after a one-time keyboard or a ForceReply reply markup has been used. UpdateChatReplyMarkup will be sent if the reply markup will be changed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteChatReplyMarkup extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The message identifier of the used keyboard.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public DeleteChatReplyMarkup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageId The message identifier of the used keyboard.
         */
        public DeleteChatReplyMarkup(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 100637531;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 100637531;
        }
    }

    /**
     * Deletes a file from the TDLib file cache.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteFile extends Function {
        /**
         * Identifier of the file to delete.
         */
        public int fileId;

        /**
         * Default constructor.
         */
        public DeleteFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId Identifier of the file to delete.
         */
        public DeleteFile(int fileId) {
            this.fileId = fileId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1807653676;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1807653676;
        }
    }

    /**
     * Deletes messages.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteMessages extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifiers of the messages to be deleted.
         */
        public long[] messageIds;
        /**
         * Pass true to try to delete outgoing messages for all chat members (may fail if messages are too old). Always true for supergroups, channels and secret chats.
         */
        public boolean revoke;

        /**
         * Default constructor.
         */
        public DeleteMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageIds Identifiers of the messages to be deleted.
         * @param revoke Pass true to try to delete outgoing messages for all chat members (may fail if messages are too old). Always true for supergroups, channels and secret chats.
         */
        public DeleteMessages(long chatId, long[] messageIds, boolean revoke) {
            this.chatId = chatId;
            this.messageIds = messageIds;
            this.revoke = revoke;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1130090173;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1130090173;
        }
    }

    /**
     * Deletes a profile photo. If something changes, updateUser will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteProfilePhoto extends Function {
        /**
         * Identifier of the profile photo to delete.
         */
        public long profilePhotoId;

        /**
         * Default constructor.
         */
        public DeleteProfilePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param profilePhotoId Identifier of the profile photo to delete.
         */
        public DeleteProfilePhoto(long profilePhotoId) {
            this.profilePhotoId = profilePhotoId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1319794625;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1319794625;
        }
    }

    /**
     * Deletes saved credentials for all payment provider bots.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteSavedCredentials extends Function {

        /**
         * Default constructor.
         */
        public DeleteSavedCredentials() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 826300114;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 826300114;
        }
    }

    /**
     * Deletes saved order info.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteSavedOrderInfo extends Function {

        /**
         * Default constructor.
         */
        public DeleteSavedOrderInfo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1629058164;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1629058164;
        }
    }

    /**
     * Deletes a supergroup or channel along with all messages in the corresponding chat. This will release the supergroup or channel username and remove all members; requires creator privileges in the supergroup or channel. Chats with more than 1000 members can't be deleted using this method.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DeleteSupergroup extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;

        /**
         * Default constructor.
         */
        public DeleteSupergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         */
        public DeleteSupergroup(int supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1999855965;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1999855965;
        }
    }

    /**
     * Closes the TDLib instance, destroying all local data without a proper logout. The current user session will remain in the list of all active sessions. All local data will be destroyed. After the destruction completes updateAuthorizationState with authorizationStateClosed will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class Destroy extends Function {

        /**
         * Default constructor.
         */
        public Destroy() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 685331274;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 685331274;
        }
    }

    /**
     * Discards a call.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DiscardCall extends Function {
        /**
         * Call identifier.
         */
        public int callId;
        /**
         * True, if the user was disconnected.
         */
        public boolean isDisconnected;
        /**
         * The call duration, in seconds.
         */
        public int duration;
        /**
         * Identifier of the connection used during the call.
         */
        public long connectionId;

        /**
         * Default constructor.
         */
        public DiscardCall() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param callId Call identifier.
         * @param isDisconnected True, if the user was disconnected.
         * @param duration The call duration, in seconds.
         * @param connectionId Identifier of the connection used during the call.
         */
        public DiscardCall(int callId, boolean isDisconnected, int duration, long connectionId) {
            this.callId = callId;
            this.isDisconnected = isDisconnected;
            this.duration = duration;
            this.connectionId = connectionId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -923187372;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -923187372;
        }
    }

    /**
     * Disconnects all websites from the current user's Telegram account.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DisconnectAllWebsites extends Function {

        /**
         * Default constructor.
         */
        public DisconnectAllWebsites() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1082985981;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1082985981;
        }
    }

    /**
     * Disconnects website from the current user's Telegram account.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class DisconnectWebsite extends Function {
        /**
         * Website identifier.
         */
        public long websiteId;

        /**
         * Default constructor.
         */
        public DisconnectWebsite() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param websiteId Website identifier.
         */
        public DisconnectWebsite(long websiteId) {
            this.websiteId = websiteId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -778767395;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -778767395;
        }
    }

    /**
     * Asynchronously downloads a file from the cloud. updateFile will be used to notify about the download progress and successful completion of the download. Returns file state just after the download has been started.
     *
     * <p> Returns {@link File File} </p>
     */
    public static class DownloadFile extends Function {
        /**
         * Identifier of the file to download.
         */
        public int fileId;
        /**
         * Priority of the download (1-32). The higher the priority, the earlier the file will be downloaded. If the priorities of two files are equal, then the last one for which downloadFile was called will be downloaded first.
         */
        public int priority;

        /**
         * Default constructor.
         */
        public DownloadFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId Identifier of the file to download.
         * @param priority Priority of the download (1-32). The higher the priority, the earlier the file will be downloaded. If the priorities of two files are equal, then the last one for which downloadFile was called will be downloaded first.
         */
        public DownloadFile(int fileId, int priority) {
            this.fileId = fileId;
            this.priority = priority;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1531851978;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1531851978;
        }
    }

    /**
     * Edits the caption of an inline message sent via a bot; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class EditInlineMessageCaption extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * New message reply markup.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New message content caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public EditInlineMessageCaption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param replyMarkup New message reply markup.
         * @param caption New message content caption; 0-200 characters.
         */
        public EditInlineMessageCaption(String inlineMessageId, ReplyMarkup replyMarkup, FormattedText caption) {
            this.inlineMessageId = inlineMessageId;
            this.replyMarkup = replyMarkup;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -760985929;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -760985929;
        }
    }

    /**
     * Edits the content of a live location in an inline message sent via a bot; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class EditInlineMessageLiveLocation extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * New message reply markup.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New location content of the message; may be null. Pass null to stop sharing the live location.
         */
        public @Nullable Location location;

        /**
         * Default constructor.
         */
        public EditInlineMessageLiveLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param replyMarkup New message reply markup.
         * @param location New location content of the message; may be null. Pass null to stop sharing the live location.
         */
        public EditInlineMessageLiveLocation(String inlineMessageId, ReplyMarkup replyMarkup, Location location) {
            this.inlineMessageId = inlineMessageId;
            this.replyMarkup = replyMarkup;
            this.location = location;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 655046316;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 655046316;
        }
    }

    /**
     * Edits the reply markup of an inline message sent via a bot; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class EditInlineMessageReplyMarkup extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * New message reply markup.
         */
        public ReplyMarkup replyMarkup;

        /**
         * Default constructor.
         */
        public EditInlineMessageReplyMarkup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param replyMarkup New message reply markup.
         */
        public EditInlineMessageReplyMarkup(String inlineMessageId, ReplyMarkup replyMarkup) {
            this.inlineMessageId = inlineMessageId;
            this.replyMarkup = replyMarkup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -67565858;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -67565858;
        }
    }

    /**
     * Edits the text of an inline text or game message sent via a bot; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class EditInlineMessageText extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * New message reply markup.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New text content of the message. Should be of type InputMessageText.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public EditInlineMessageText() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param replyMarkup New message reply markup.
         * @param inputMessageContent New text content of the message. Should be of type InputMessageText.
         */
        public EditInlineMessageText(String inlineMessageId, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.inlineMessageId = inlineMessageId;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -855457307;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -855457307;
        }
    }

    /**
     * Edits the message content caption. Non-bots can edit messages for a limited period of time. Returns the edited message after the edit is completed server-side.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class EditMessageCaption extends Function {
        /**
         * The chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * The new message reply markup; for bots only.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New message content caption; 0-200 characters.
         */
        public FormattedText caption;

        /**
         * Default constructor.
         */
        public EditMessageCaption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat the message belongs to.
         * @param messageId Identifier of the message.
         * @param replyMarkup The new message reply markup; for bots only.
         * @param caption New message content caption; 0-200 characters.
         */
        public EditMessageCaption(long chatId, long messageId, ReplyMarkup replyMarkup, FormattedText caption) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.replyMarkup = replyMarkup;
            this.caption = caption;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1154677038;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1154677038;
        }
    }

    /**
     * Edits the message content of a live location. Messages can be edited for a limited period of time specified in the live location. Returns the edited message after the edit is completed server-side.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class EditMessageLiveLocation extends Function {
        /**
         * The chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * Tew message reply markup; for bots only.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New location content of the message; may be null. Pass null to stop sharing the live location.
         */
        public @Nullable Location location;

        /**
         * Default constructor.
         */
        public EditMessageLiveLocation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat the message belongs to.
         * @param messageId Identifier of the message.
         * @param replyMarkup Tew message reply markup; for bots only.
         * @param location New location content of the message; may be null. Pass null to stop sharing the live location.
         */
        public EditMessageLiveLocation(long chatId, long messageId, ReplyMarkup replyMarkup, Location location) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.replyMarkup = replyMarkup;
            this.location = location;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1146772745;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1146772745;
        }
    }

    /**
     * Edits the message reply markup; for bots only. Returns the edited message after the edit is completed server-side.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class EditMessageReplyMarkup extends Function {
        /**
         * The chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * New message reply markup.
         */
        public ReplyMarkup replyMarkup;

        /**
         * Default constructor.
         */
        public EditMessageReplyMarkup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat the message belongs to.
         * @param messageId Identifier of the message.
         * @param replyMarkup New message reply markup.
         */
        public EditMessageReplyMarkup(long chatId, long messageId, ReplyMarkup replyMarkup) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.replyMarkup = replyMarkup;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 332127881;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 332127881;
        }
    }

    /**
     * Edits the text of a message (or a text of a game message). Non-bot users can edit messages for a limited period of time. Returns the edited message after the edit is completed on the server side.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class EditMessageText extends Function {
        /**
         * The chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * The new message reply markup; for bots only.
         */
        public ReplyMarkup replyMarkup;
        /**
         * New text content of the message. Should be of type InputMessageText.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public EditMessageText() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat the message belongs to.
         * @param messageId Identifier of the message.
         * @param replyMarkup The new message reply markup; for bots only.
         * @param inputMessageContent New text content of the message. Should be of type InputMessageText.
         */
        public EditMessageText(long chatId, long messageId, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 196272567;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 196272567;
        }
    }

    /**
     * Finishes the file generation.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class FinishFileGeneration extends Function {
        /**
         * The identifier of the generation process.
         */
        public long generationId;
        /**
         * If set, means that file generation has failed and should be terminated.
         */
        public Error error;

        /**
         * Default constructor.
         */
        public FinishFileGeneration() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param generationId The identifier of the generation process.
         * @param error If set, means that file generation has failed and should be terminated.
         */
        public FinishFileGeneration(long generationId, Error error) {
            this.generationId = generationId;
            this.error = error;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1055060835;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1055060835;
        }
    }

    /**
     * Forwards previously sent messages. Returns the forwarded messages in the same order as the message identifiers passed in messageIds. If a message can't be forwarded, null will be returned instead of the message.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class ForwardMessages extends Function {
        /**
         * Identifier of the chat to which to forward messages.
         */
        public long chatId;
        /**
         * Identifier of the chat from which to forward messages.
         */
        public long fromChatId;
        /**
         * Identifiers of the messages to forward.
         */
        public long[] messageIds;
        /**
         * Pass true to disable notification for the message, doesn't work if messages are forwarded to a secret chat.
         */
        public boolean disableNotification;
        /**
         * Pass true if the message is sent from the background.
         */
        public boolean fromBackground;
        /**
         * True, if the messages should be grouped into an album after forwarding. For this to work, no more than 10 messages may be forwarded, and all of them must be photo or video messages.
         */
        public boolean asAlbum;

        /**
         * Default constructor.
         */
        public ForwardMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat to which to forward messages.
         * @param fromChatId Identifier of the chat from which to forward messages.
         * @param messageIds Identifiers of the messages to forward.
         * @param disableNotification Pass true to disable notification for the message, doesn't work if messages are forwarded to a secret chat.
         * @param fromBackground Pass true if the message is sent from the background.
         * @param asAlbum True, if the messages should be grouped into an album after forwarding. For this to work, no more than 10 messages may be forwarded, and all of them must be photo or video messages.
         */
        public ForwardMessages(long chatId, long fromChatId, long[] messageIds, boolean disableNotification, boolean fromBackground, boolean asAlbum) {
            this.chatId = chatId;
            this.fromChatId = fromChatId;
            this.messageIds = messageIds;
            this.disableNotification = disableNotification;
            this.fromBackground = fromBackground;
            this.asAlbum = asAlbum;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -537573308;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -537573308;
        }
    }

    /**
     * Generates a new invite link for a chat; the previously generated link is revoked. Available for basic groups, supergroups, and channels. In basic groups this can be called only by the group's creator; in supergroups and channels this requires appropriate administrator rights.
     *
     * <p> Returns {@link ChatInviteLink ChatInviteLink} </p>
     */
    public static class GenerateChatInviteLink extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public GenerateChatInviteLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public GenerateChatInviteLink(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1945532500;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1945532500;
        }
    }

    /**
     * Returns the period of inactivity after which the account of the current user will automatically be deleted.
     *
     * <p> Returns {@link AccountTtl AccountTtl} </p>
     */
    public static class GetAccountTtl extends Function {

        /**
         * Default constructor.
         */
        public GetAccountTtl() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -443905161;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -443905161;
        }
    }

    /**
     * Returns all active live locations that should be updated by the client. The list is persistent across application restarts only if the message database is used.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class GetActiveLiveLocationMessages extends Function {

        /**
         * Default constructor.
         */
        public GetActiveLiveLocationMessages() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1425459567;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1425459567;
        }
    }

    /**
     * Returns all active sessions of the current user.
     *
     * <p> Returns {@link Sessions Sessions} </p>
     */
    public static class GetActiveSessions extends Function {

        /**
         * Default constructor.
         */
        public GetActiveSessions() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1119710526;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1119710526;
        }
    }

    /**
     * Returns a list of archived sticker sets.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class GetArchivedStickerSets extends Function {
        /**
         * Pass true to return mask stickers sets; pass false to return ordinary sticker sets.
         */
        public boolean isMasks;
        /**
         * Identifier of the sticker set from which to return the result.
         */
        public long offsetStickerSetId;
        /**
         * Maximum number of sticker sets to return.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetArchivedStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMasks Pass true to return mask stickers sets; pass false to return ordinary sticker sets.
         * @param offsetStickerSetId Identifier of the sticker set from which to return the result.
         * @param limit Maximum number of sticker sets to return.
         */
        public GetArchivedStickerSets(boolean isMasks, long offsetStickerSetId, int limit) {
            this.isMasks = isMasks;
            this.offsetStickerSetId = offsetStickerSetId;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1996943238;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1996943238;
        }
    }

    /**
     * Returns a list of sticker sets attached to a file. Currently only photos and videos can have attached sticker sets.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class GetAttachedStickerSets extends Function {
        /**
         * File identifier.
         */
        public int fileId;

        /**
         * Default constructor.
         */
        public GetAttachedStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId File identifier.
         */
        public GetAttachedStickerSets(int fileId) {
            this.fileId = fileId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1302172429;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1302172429;
        }
    }

    /**
     * Returns the current authorization state; this is an offline request. For informational purposes only. Use updateAuthorizationState instead to maintain the current authorization state.
     *
     * <p> Returns {@link AuthorizationState AuthorizationState} </p>
     */
    public static class GetAuthorizationState extends Function {

        /**
         * Default constructor.
         */
        public GetAuthorizationState() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1949154877;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1949154877;
        }
    }

    /**
     * Returns information about a basic group by its identifier. This is an offline request if the current user is not a bot.
     *
     * <p> Returns {@link BasicGroup BasicGroup} </p>
     */
    public static class GetBasicGroup extends Function {
        /**
         * Basic group identifier.
         */
        public int basicGroupId;

        /**
         * Default constructor.
         */
        public GetBasicGroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Basic group identifier.
         */
        public GetBasicGroup(int basicGroupId) {
            this.basicGroupId = basicGroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 561775568;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 561775568;
        }
    }

    /**
     * Returns full information about a basic group by its identifier.
     *
     * <p> Returns {@link BasicGroupFullInfo BasicGroupFullInfo} </p>
     */
    public static class GetBasicGroupFullInfo extends Function {
        /**
         * Basic group identifier.
         */
        public int basicGroupId;

        /**
         * Default constructor.
         */
        public GetBasicGroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Basic group identifier.
         */
        public GetBasicGroupFullInfo(int basicGroupId) {
            this.basicGroupId = basicGroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1770517905;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1770517905;
        }
    }

    /**
     * Returns users that were blocked by the current user.
     *
     * <p> Returns {@link Users Users} </p>
     */
    public static class GetBlockedUsers extends Function {
        /**
         * Number of users to skip in the result; must be non-negative.
         */
        public int offset;
        /**
         * Maximum number of users to return; up to 100.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetBlockedUsers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param offset Number of users to skip in the result; must be non-negative.
         * @param limit Maximum number of users to return; up to 100.
         */
        public GetBlockedUsers(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -742912777;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -742912777;
        }
    }

    /**
     * Sends a callback query to a bot and returns an answer. Returns an error with code 502 if the bot fails to answer the query before the query timeout expires.
     *
     * <p> Returns {@link CallbackQueryAnswer CallbackQueryAnswer} </p>
     */
    public static class GetCallbackQueryAnswer extends Function {
        /**
         * Identifier of the chat with the message.
         */
        public long chatId;
        /**
         * Identifier of the message from which the query originated.
         */
        public long messageId;
        /**
         * Query payload.
         */
        public CallbackQueryPayload payload;

        /**
         * Default constructor.
         */
        public GetCallbackQueryAnswer() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat with the message.
         * @param messageId Identifier of the message from which the query originated.
         * @param payload Query payload.
         */
        public GetCallbackQueryAnswer(long chatId, long messageId, CallbackQueryPayload payload) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.payload = payload;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 116357727;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 116357727;
        }
    }

    /**
     * Returns information about a chat by its identifier, this is an offline request if the current user is not a bot.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class GetChat extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public GetChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public GetChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1866601536;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1866601536;
        }
    }

    /**
     * Returns a list of users who are administrators of the chat.
     *
     * <p> Returns {@link Users Users} </p>
     */
    public static class GetChatAdministrators extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public GetChatAdministrators() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public GetChatAdministrators(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 508231041;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 508231041;
        }
    }

    /**
     * Returns a list of service actions taken by chat members and administrators in the last 48 hours. Available only in supergroups and channels. Requires administrator rights. Returns results in reverse chronological order (i. e., in order of decreasing eventId).
     *
     * <p> Returns {@link ChatEvents ChatEvents} </p>
     */
    public static class GetChatEventLog extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Search query by which to filter events.
         */
        public String query;
        /**
         * Identifier of an event from which to return results. Use 0 to get results from the latest events.
         */
        public long fromEventId;
        /**
         * Maximum number of events to return; up to 100.
         */
        public int limit;
        /**
         * The types of events to return. By default, all types will be returned.
         */
        public ChatEventLogFilters filters;
        /**
         * User identifiers by which to filter events. By default, events relating to all users will be returned.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public GetChatEventLog() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param query Search query by which to filter events.
         * @param fromEventId Identifier of an event from which to return results. Use 0 to get results from the latest events.
         * @param limit Maximum number of events to return; up to 100.
         * @param filters The types of events to return. By default, all types will be returned.
         * @param userIds User identifiers by which to filter events. By default, events relating to all users will be returned.
         */
        public GetChatEventLog(long chatId, String query, long fromEventId, int limit, ChatEventLogFilters filters, int[] userIds) {
            this.chatId = chatId;
            this.query = query;
            this.fromEventId = fromEventId;
            this.limit = limit;
            this.filters = filters;
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 206900967;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 206900967;
        }
    }

    /**
     * Returns messages in a chat. The messages are returned in a reverse chronological order (i.e., in order of decreasing messageId). For optimal performance the number of returned messages is chosen by the library. This is an offline request if onlyLocal is true.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class GetChatHistory extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Identifier of the message starting from which history must be fetched; use 0 to get results from the beginning (i.e., from oldest to newest).
         */
        public long fromMessageId;
        /**
         * Specify 0 to get results from exactly the fromMessageId or a negative offset to get the specified message and some newer messages.
         */
        public int offset;
        /**
         * The maximum number of messages to be returned; must be positive and can't be greater than 100. If the offset is negative, the limit must be greater than -offset. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public int limit;
        /**
         * If true, returns only messages that are available locally without sending network requests.
         */
        public boolean onlyLocal;

        /**
         * Default constructor.
         */
        public GetChatHistory() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param fromMessageId Identifier of the message starting from which history must be fetched; use 0 to get results from the beginning (i.e., from oldest to newest).
         * @param offset Specify 0 to get results from exactly the fromMessageId or a negative offset to get the specified message and some newer messages.
         * @param limit The maximum number of messages to be returned; must be positive and can't be greater than 100. If the offset is negative, the limit must be greater than -offset. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         * @param onlyLocal If true, returns only messages that are available locally without sending network requests.
         */
        public GetChatHistory(long chatId, long fromMessageId, int offset, int limit, boolean onlyLocal) {
            this.chatId = chatId;
            this.fromMessageId = fromMessageId;
            this.offset = offset;
            this.limit = limit;
            this.onlyLocal = onlyLocal;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -799960451;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -799960451;
        }
    }

    /**
     * Returns information about a single member of a chat.
     *
     * <p> Returns {@link ChatMember ChatMember} </p>
     */
    public static class GetChatMember extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public GetChatMember() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userId User identifier.
         */
        public GetChatMember(long chatId, int userId) {
            this.chatId = chatId;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 677085892;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 677085892;
        }
    }

    /**
     * Returns the last message sent in a chat no later than the specified date.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class GetChatMessageByDate extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Point in time (Unix timestamp) relative to which to search for messages.
         */
        public int date;

        /**
         * Default constructor.
         */
        public GetChatMessageByDate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param date Point in time (Unix timestamp) relative to which to search for messages.
         */
        public GetChatMessageByDate(long chatId, int date) {
            this.chatId = chatId;
            this.date = date;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1062564150;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1062564150;
        }
    }

    /**
     * Returns information about a pinned chat message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class GetChatPinnedMessage extends Function {
        /**
         * Identifier of the chat the message belongs to.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public GetChatPinnedMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat the message belongs to.
         */
        public GetChatPinnedMessage(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 359865008;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 359865008;
        }
    }

    /**
     * Returns information on whether the current chat can be reported as spam.
     *
     * <p> Returns {@link ChatReportSpamState ChatReportSpamState} </p>
     */
    public static class GetChatReportSpamState extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public GetChatReportSpamState() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public GetChatReportSpamState(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -748866856;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -748866856;
        }
    }

    /**
     * Returns an ordered list of chats. Chats are sorted by the pair (order, chatId) in decreasing order. (For example, to get a list of chats from the beginning, the offsetOrder should be equal to 2^63 - 1). For optimal performance the number of returned chats is chosen by the library.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class GetChats extends Function {
        /**
         * Chat order to return chats from.
         */
        public long offsetOrder;
        /**
         * Chat identifier to return chats from.
         */
        public long offsetChatId;
        /**
         * The maximum number of chats to be returned. It is possible that fewer chats than the limit are returned even if the end of the list is not reached.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetChats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param offsetOrder Chat order to return chats from.
         * @param offsetChatId Chat identifier to return chats from.
         * @param limit The maximum number of chats to be returned. It is possible that fewer chats than the limit are returned even if the end of the list is not reached.
         */
        public GetChats(long offsetOrder, long offsetChatId, int limit) {
            this.offsetOrder = offsetOrder;
            this.offsetChatId = offsetChatId;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2121381601;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2121381601;
        }
    }

    /**
     * Returns all website where the current user used Telegram to log in.
     *
     * <p> Returns {@link ConnectedWebsites ConnectedWebsites} </p>
     */
    public static class GetConnectedWebsites extends Function {

        /**
         * Default constructor.
         */
        public GetConnectedWebsites() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -170536110;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -170536110;
        }
    }

    /**
     * Uses current user IP to found his country. Returns two-letter ISO 3166-1 alpha-2 country code. Can be called before authorization.
     *
     * <p> Returns {@link Text Text} </p>
     */
    public static class GetCountryCode extends Function {

        /**
         * Default constructor.
         */
        public GetCountryCode() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1540593906;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1540593906;
        }
    }

    /**
     * Returns a list of public chats created by the user.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class GetCreatedPublicChats extends Function {

        /**
         * Default constructor.
         */
        public GetCreatedPublicChats() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1609082914;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1609082914;
        }
    }

    /**
     * Returns favorite stickers.
     *
     * <p> Returns {@link Stickers Stickers} </p>
     */
    public static class GetFavoriteStickers extends Function {

        /**
         * Default constructor.
         */
        public GetFavoriteStickers() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -338964672;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -338964672;
        }
    }

    /**
     * Returns information about a file; this is an offline request.
     *
     * <p> Returns {@link File File} </p>
     */
    public static class GetFile extends Function {
        /**
         * Identifier of the file to get.
         */
        public int fileId;

        /**
         * Default constructor.
         */
        public GetFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileId Identifier of the file to get.
         */
        public GetFile(int fileId) {
            this.fileId = fileId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1553923406;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1553923406;
        }
    }

    /**
     * Returns the extension of a file, guessed by its MIME type. Returns an empty string on failure. This is an offline method. Can be called before authorization. Can be called synchronously.
     *
     * <p> Returns {@link Text Text} </p>
     */
    public static class GetFileExtension extends Function {
        /**
         * The MIME type of the file.
         */
        public String mimeType;

        /**
         * Default constructor.
         */
        public GetFileExtension() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param mimeType The MIME type of the file.
         */
        public GetFileExtension(String mimeType) {
            this.mimeType = mimeType;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -106055372;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -106055372;
        }
    }

    /**
     * Returns the MIME type of a file, guessed by its extension. Returns an empty string on failure. This is an offline method. Can be called before authorization. Can be called synchronously.
     *
     * <p> Returns {@link Text Text} </p>
     */
    public static class GetFileMimeType extends Function {
        /**
         * The name of the file or path to the file.
         */
        public String fileName;

        /**
         * Default constructor.
         */
        public GetFileMimeType() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fileName The name of the file or path to the file.
         */
        public GetFileMimeType(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2073879671;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2073879671;
        }
    }

    /**
     * Returns the high scores for a game and some part of the high score table in the range of the specified user; for bots only.
     *
     * <p> Returns {@link GameHighScores GameHighScores} </p>
     */
    public static class GetGameHighScores extends Function {
        /**
         * The chat that contains the message with the game.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public GetGameHighScores() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat that contains the message with the game.
         * @param messageId Identifier of the message.
         * @param userId User identifier.
         */
        public GetGameHighScores(long chatId, long messageId, int userId) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1920923753;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1920923753;
        }
    }

    /**
     * Returns a list of common chats with a given user. Chats are sorted by their type and creation date.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class GetGroupsInCommon extends Function {
        /**
         * User identifier.
         */
        public int userId;
        /**
         * Chat identifier starting from which to return chats; use 0 for the first request.
         */
        public long offsetChatId;
        /**
         * Maximum number of chats to be returned; up to 100.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetGroupsInCommon() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         * @param offsetChatId Chat identifier starting from which to return chats; use 0 for the first request.
         * @param limit Maximum number of chats to be returned; up to 100.
         */
        public GetGroupsInCommon(int userId, long offsetChatId, int limit) {
            this.userId = userId;
            this.offsetChatId = offsetChatId;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -23238689;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -23238689;
        }
    }

    /**
     * Returns the total number of imported contacts.
     *
     * <p> Returns {@link Count Count} </p>
     */
    public static class GetImportedContactCount extends Function {

        /**
         * Default constructor.
         */
        public GetImportedContactCount() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -656336346;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -656336346;
        }
    }

    /**
     * Returns game high scores and some part of the high score table in the range of the specified user; for bots only.
     *
     * <p> Returns {@link GameHighScores GameHighScores} </p>
     */
    public static class GetInlineGameHighScores extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public GetInlineGameHighScores() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param userId User identifier.
         */
        public GetInlineGameHighScores(String inlineMessageId, int userId) {
            this.inlineMessageId = inlineMessageId;
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1833445800;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1833445800;
        }
    }

    /**
     * Sends an inline query to a bot and returns its results. Returns an error with code 502 if the bot fails to answer the query before the query timeout expires.
     *
     * <p> Returns {@link InlineQueryResults InlineQueryResults} </p>
     */
    public static class GetInlineQueryResults extends Function {
        /**
         * The identifier of the target bot.
         */
        public int botUserId;
        /**
         * Identifier of the chat, where the query was sent.
         */
        public long chatId;
        /**
         * Location of the user, only if needed.
         */
        public Location userLocation;
        /**
         * Text of the query.
         */
        public String query;
        /**
         * Offset of the first entry to return.
         */
        public String offset;

        /**
         * Default constructor.
         */
        public GetInlineQueryResults() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param botUserId The identifier of the target bot.
         * @param chatId Identifier of the chat, where the query was sent.
         * @param userLocation Location of the user, only if needed.
         * @param query Text of the query.
         * @param offset Offset of the first entry to return.
         */
        public GetInlineQueryResults(int botUserId, long chatId, Location userLocation, String query, String offset) {
            this.botUserId = botUserId;
            this.chatId = chatId;
            this.userLocation = userLocation;
            this.query = query;
            this.offset = offset;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1182511172;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1182511172;
        }
    }

    /**
     * Returns a list of installed sticker sets.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class GetInstalledStickerSets extends Function {
        /**
         * Pass true to return mask sticker sets; pass false to return ordinary sticker sets.
         */
        public boolean isMasks;

        /**
         * Default constructor.
         */
        public GetInstalledStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMasks Pass true to return mask sticker sets; pass false to return ordinary sticker sets.
         */
        public GetInstalledStickerSets(boolean isMasks) {
            this.isMasks = isMasks;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1214523749;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1214523749;
        }
    }

    /**
     * Returns the default text for invitation messages to be used as a placeholder when the current user invites friends to Telegram.
     *
     * <p> Returns {@link Text Text} </p>
     */
    public static class GetInviteText extends Function {

        /**
         * Default constructor.
         */
        public GetInviteText() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 794573512;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 794573512;
        }
    }

    /**
     * Returns the current user.
     *
     * <p> Returns {@link User User} </p>
     */
    public static class GetMe extends Function {

        /**
         * Default constructor.
         */
        public GetMe() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -191516033;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -191516033;
        }
    }

    /**
     * Returns information about a message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class GetMessage extends Function {
        /**
         * Identifier of the chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message to get.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public GetMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat the message belongs to.
         * @param messageId Identifier of the message to get.
         */
        public GetMessage(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1821196160;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1821196160;
        }
    }

    /**
     * Returns information about messages. If a message is not found, returns null on the corresponding position of the result.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class GetMessages extends Function {
        /**
         * Identifier of the chat the messages belong to.
         */
        public long chatId;
        /**
         * Identifiers of the messages to get.
         */
        public long[] messageIds;

        /**
         * Default constructor.
         */
        public GetMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat the messages belong to.
         * @param messageIds Identifiers of the messages to get.
         */
        public GetMessages(long chatId, long[] messageIds) {
            this.chatId = chatId;
            this.messageIds = messageIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 425299338;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 425299338;
        }
    }

    /**
     * Returns network data usage statistics. Can be called before authorization.
     *
     * <p> Returns {@link NetworkStatistics NetworkStatistics} </p>
     */
    public static class GetNetworkStatistics extends Function {
        /**
         * If true, returns only data for the current library launch.
         */
        public boolean onlyCurrent;

        /**
         * Default constructor.
         */
        public GetNetworkStatistics() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param onlyCurrent If true, returns only data for the current library launch.
         */
        public GetNetworkStatistics(boolean onlyCurrent) {
            this.onlyCurrent = onlyCurrent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -986228706;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -986228706;
        }
    }

    /**
     * Returns the notification settings for a given scope.
     *
     * <p> Returns {@link NotificationSettings NotificationSettings} </p>
     */
    public static class GetNotificationSettings extends Function {
        /**
         * Scope for which to return the notification settings information.
         */
        public NotificationSettingsScope scope;

        /**
         * Default constructor.
         */
        public GetNotificationSettings() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param scope Scope for which to return the notification settings information.
         */
        public GetNotificationSettings(NotificationSettingsScope scope) {
            this.scope = scope;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 907144391;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 907144391;
        }
    }

    /**
     * Returns the value of an option by its name. (Check the list of available options on https://core.telegram.org/tdlib/options.) Can be called before authorization.
     *
     * <p> Returns {@link OptionValue OptionValue} </p>
     */
    public static class GetOption extends Function {
        /**
         * The name of the option.
         */
        public String name;

        /**
         * Default constructor.
         */
        public GetOption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name The name of the option.
         */
        public GetOption(String name) {
            this.name = name;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1572495746;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1572495746;
        }
    }

    /**
     * Returns the current state of 2-step verification.
     *
     * <p> Returns {@link PasswordState PasswordState} </p>
     */
    public static class GetPasswordState extends Function {

        /**
         * Default constructor.
         */
        public GetPasswordState() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -174752904;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -174752904;
        }
    }

    /**
     * Returns an invoice payment form. This method should be called when the user presses inlineKeyboardButtonBuy.
     *
     * <p> Returns {@link PaymentForm PaymentForm} </p>
     */
    public static class GetPaymentForm extends Function {
        /**
         * Chat identifier of the Invoice message.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public GetPaymentForm() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the Invoice message.
         * @param messageId Message identifier.
         */
        public GetPaymentForm(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2146950882;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2146950882;
        }
    }

    /**
     * Returns information about a successful payment.
     *
     * <p> Returns {@link PaymentReceipt PaymentReceipt} </p>
     */
    public static class GetPaymentReceipt extends Function {
        /**
         * Chat identifier of the PaymentSuccessful message.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public GetPaymentReceipt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the PaymentSuccessful message.
         * @param messageId Message identifier.
         */
        public GetPaymentReceipt(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1013758294;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1013758294;
        }
    }

    /**
     * Returns the proxy that is currently set up. Can be called before authorization.
     *
     * <p> Returns {@link Proxy Proxy} </p>
     */
    public static class GetProxy extends Function {

        /**
         * Default constructor.
         */
        public GetProxy() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1389343170;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1389343170;
        }
    }

    /**
     * Returns a public HTTPS link to a message. Available only for messages in public supergroups and channels.
     *
     * <p> Returns {@link PublicMessageLink PublicMessageLink} </p>
     */
    public static class GetPublicMessageLink extends Function {
        /**
         * Identifier of the chat to which the message belongs.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * Pass true if a link for a whole media album should be returned.
         */
        public boolean forAlbum;

        /**
         * Default constructor.
         */
        public GetPublicMessageLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat to which the message belongs.
         * @param messageId Identifier of the message.
         * @param forAlbum Pass true if a link for a whole media album should be returned.
         */
        public GetPublicMessageLink(long chatId, long messageId, boolean forAlbum) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.forAlbum = forAlbum;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -374642839;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -374642839;
        }
    }

    /**
     * Returns up to 20 recently used inline bots in the order of their last usage.
     *
     * <p> Returns {@link Users Users} </p>
     */
    public static class GetRecentInlineBots extends Function {

        /**
         * Default constructor.
         */
        public GetRecentInlineBots() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1437823548;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1437823548;
        }
    }

    /**
     * Returns a list of recently used stickers.
     *
     * <p> Returns {@link Stickers Stickers} </p>
     */
    public static class GetRecentStickers extends Function {
        /**
         * Pass true to return stickers and masks that were recently attached to photos or video files; pass false to return recently sent stickers.
         */
        public boolean isAttached;

        /**
         * Default constructor.
         */
        public GetRecentStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAttached Pass true to return stickers and masks that were recently attached to photos or video files; pass false to return recently sent stickers.
         */
        public GetRecentStickers(boolean isAttached) {
            this.isAttached = isAttached;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -579622241;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -579622241;
        }
    }

    /**
     * Returns t.me URLs recently visited by a newly registered user.
     *
     * <p> Returns {@link TMeUrls TMeUrls} </p>
     */
    public static class GetRecentlyVisitedTMeUrls extends Function {
        /**
         * Google Play referrer to identify the user.
         */
        public String referrer;

        /**
         * Default constructor.
         */
        public GetRecentlyVisitedTMeUrls() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param referrer Google Play referrer to identify the user.
         */
        public GetRecentlyVisitedTMeUrls(String referrer) {
            this.referrer = referrer;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 806754961;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 806754961;
        }
    }

    /**
     * Returns a recovery email address that was previously set up. This method can be used to verify a password provided by the user.
     *
     * <p> Returns {@link RecoveryEmailAddress RecoveryEmailAddress} </p>
     */
    public static class GetRecoveryEmailAddress extends Function {
        /**
         * The password for the current user.
         */
        public String password;

        /**
         * Default constructor.
         */
        public GetRecoveryEmailAddress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param password The password for the current user.
         */
        public GetRecoveryEmailAddress(String password) {
            this.password = password;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1594770947;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1594770947;
        }
    }

    /**
     * Returns information about a file by its remote ID; this is an offline request. Can be used to register a URL as a file for further uploading, or sending as a message.
     *
     * <p> Returns {@link File File} </p>
     */
    public static class GetRemoteFile extends Function {
        /**
         * Remote identifier of the file to get.
         */
        public String remoteFileId;
        /**
         * File type, if known.
         */
        public FileType fileType;

        /**
         * Default constructor.
         */
        public GetRemoteFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param remoteFileId Remote identifier of the file to get.
         * @param fileType File type, if known.
         */
        public GetRemoteFile(String remoteFileId, FileType fileType) {
            this.remoteFileId = remoteFileId;
            this.fileType = fileType;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2137204530;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2137204530;
        }
    }

    /**
     * Returns information about a message that is replied by given message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class GetRepliedMessage extends Function {
        /**
         * Identifier of the chat the message belongs to.
         */
        public long chatId;
        /**
         * Identifier of the message reply to which get.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public GetRepliedMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat the message belongs to.
         * @param messageId Identifier of the message reply to which get.
         */
        public GetRepliedMessage(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -641918531;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -641918531;
        }
    }

    /**
     * Returns saved animations.
     *
     * <p> Returns {@link Animations Animations} </p>
     */
    public static class GetSavedAnimations extends Function {

        /**
         * Default constructor.
         */
        public GetSavedAnimations() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 7051032;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 7051032;
        }
    }

    /**
     * Returns saved order info, if any.
     *
     * <p> Returns {@link OrderInfo OrderInfo} </p>
     */
    public static class GetSavedOrderInfo extends Function {

        /**
         * Default constructor.
         */
        public GetSavedOrderInfo() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1152016675;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1152016675;
        }
    }

    /**
     * Returns information about a secret chat by its identifier. This is an offline request.
     *
     * <p> Returns {@link SecretChat SecretChat} </p>
     */
    public static class GetSecretChat extends Function {
        /**
         * Secret chat identifier.
         */
        public int secretChatId;

        /**
         * Default constructor.
         */
        public GetSecretChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param secretChatId Secret chat identifier.
         */
        public GetSecretChat(int secretChatId) {
            this.secretChatId = secretChatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 40599169;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 40599169;
        }
    }

    /**
     * Returns emoji corresponding to a sticker.
     *
     * <p> Returns {@link StickerEmojis StickerEmojis} </p>
     */
    public static class GetStickerEmojis extends Function {
        /**
         * Sticker file identifier.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public GetStickerEmojis() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker file identifier.
         */
        public GetStickerEmojis(InputFile sticker) {
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 95352475;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 95352475;
        }
    }

    /**
     * Returns information about a sticker set by its identifier.
     *
     * <p> Returns {@link StickerSet StickerSet} </p>
     */
    public static class GetStickerSet extends Function {
        /**
         * Identifier of the sticker set.
         */
        public long setId;

        /**
         * Default constructor.
         */
        public GetStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setId Identifier of the sticker set.
         */
        public GetStickerSet(long setId) {
            this.setId = setId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1052318659;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1052318659;
        }
    }

    /**
     * Returns stickers from the installed sticker sets that correspond to a given emoji. If the emoji is not empty, favorite and recently used stickers may also be returned.
     *
     * <p> Returns {@link Stickers Stickers} </p>
     */
    public static class GetStickers extends Function {
        /**
         * String representation of emoji. If empty, returns all known installed stickers.
         */
        public String emoji;
        /**
         * Maximum number of stickers to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param emoji String representation of emoji. If empty, returns all known installed stickers.
         * @param limit Maximum number of stickers to be returned.
         */
        public GetStickers(String emoji, int limit) {
            this.emoji = emoji;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1594919556;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1594919556;
        }
    }

    /**
     * Returns storage usage statistics.
     *
     * <p> Returns {@link StorageStatistics StorageStatistics} </p>
     */
    public static class GetStorageStatistics extends Function {
        /**
         * Maximum number of chats with the largest storage usage for which separate statistics should be returned. All other chats will be grouped in entries with chatId == 0. If the chat info database is not used, the chatLimit is ignored and is always set to 0.
         */
        public int chatLimit;

        /**
         * Default constructor.
         */
        public GetStorageStatistics() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatLimit Maximum number of chats with the largest storage usage for which separate statistics should be returned. All other chats will be grouped in entries with chatId == 0. If the chat info database is not used, the chatLimit is ignored and is always set to 0.
         */
        public GetStorageStatistics(int chatLimit) {
            this.chatLimit = chatLimit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -853193929;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -853193929;
        }
    }

    /**
     * Quickly returns approximate storage usage statistics.
     *
     * <p> Returns {@link StorageStatisticsFast StorageStatisticsFast} </p>
     */
    public static class GetStorageStatisticsFast extends Function {

        /**
         * Default constructor.
         */
        public GetStorageStatisticsFast() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 61368066;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 61368066;
        }
    }

    /**
     * Returns information about a supergroup or channel by its identifier. This is an offline request if the current user is not a bot.
     *
     * <p> Returns {@link Supergroup Supergroup} </p>
     */
    public static class GetSupergroup extends Function {
        /**
         * Supergroup or channel identifier.
         */
        public int supergroupId;

        /**
         * Default constructor.
         */
        public GetSupergroup() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Supergroup or channel identifier.
         */
        public GetSupergroup(int supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2063063706;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2063063706;
        }
    }

    /**
     * Returns full information about a supergroup or channel by its identifier, cached for up to 1 minute.
     *
     * <p> Returns {@link SupergroupFullInfo SupergroupFullInfo} </p>
     */
    public static class GetSupergroupFullInfo extends Function {
        /**
         * Supergroup or channel identifier.
         */
        public int supergroupId;

        /**
         * Default constructor.
         */
        public GetSupergroupFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Supergroup or channel identifier.
         */
        public GetSupergroupFullInfo(int supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1150331262;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1150331262;
        }
    }

    /**
     * Returns information about members or banned users in a supergroup or channel. Can be used only if SupergroupFullInfo.canGetMembers == true; additionally, administrator privileges may be required for some filters.
     *
     * <p> Returns {@link ChatMembers ChatMembers} </p>
     */
    public static class GetSupergroupMembers extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;
        /**
         * The type of users to return. By default, supergroupMembersRecent.
         */
        public SupergroupMembersFilter filter;
        /**
         * Number of users to skip.
         */
        public int offset;
        /**
         * The maximum number of users be returned; up to 200.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetSupergroupMembers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         * @param filter The type of users to return. By default, supergroupMembersRecent.
         * @param offset Number of users to skip.
         * @param limit The maximum number of users be returned; up to 200.
         */
        public GetSupergroupMembers(int supergroupId, SupergroupMembersFilter filter, int offset, int limit) {
            this.supergroupId = supergroupId;
            this.filter = filter;
            this.offset = offset;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1427643098;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1427643098;
        }
    }

    /**
     * Returns a user that can be contacted to get support.
     *
     * <p> Returns {@link User User} </p>
     */
    public static class GetSupportUser extends Function {

        /**
         * Default constructor.
         */
        public GetSupportUser() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1733497700;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1733497700;
        }
    }

    /**
     * Returns information about the current temporary password.
     *
     * <p> Returns {@link TemporaryPasswordState TemporaryPasswordState} </p>
     */
    public static class GetTemporaryPasswordState extends Function {

        /**
         * Default constructor.
         */
        public GetTemporaryPasswordState() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -12670830;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -12670830;
        }
    }

    /**
     * Returns the terms of service. Can be called before authorization.
     *
     * <p> Returns {@link Text Text} </p>
     */
    public static class GetTermsOfService extends Function {

        /**
         * Default constructor.
         */
        public GetTermsOfService() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1835034646;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1835034646;
        }
    }

    /**
     * Returns all entities (mentions, hashtags, cashtags, bot commands, URLs, and email addresses) contained in the text. This is an offline method. Can be called before authorization. Can be called synchronously.
     *
     * <p> Returns {@link TextEntities TextEntities} </p>
     */
    public static class GetTextEntities extends Function {
        /**
         * The text in which to look for entites.
         */
        public String text;

        /**
         * Default constructor.
         */
        public GetTextEntities() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text The text in which to look for entites.
         */
        public GetTextEntities(String text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -341490693;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -341490693;
        }
    }

    /**
     * Returns a list of frequently used chats. Supported only if the chat info database is enabled.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class GetTopChats extends Function {
        /**
         * Category of chats to be returned.
         */
        public TopChatCategory category;
        /**
         * Maximum number of chats to be returned; up to 30.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetTopChats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param category Category of chats to be returned.
         * @param limit Maximum number of chats to be returned; up to 30.
         */
        public GetTopChats(TopChatCategory category, int limit) {
            this.category = category;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -388410847;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -388410847;
        }
    }

    /**
     * Returns a list of trending sticker sets.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class GetTrendingStickerSets extends Function {

        /**
         * Default constructor.
         */
        public GetTrendingStickerSets() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1729129957;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1729129957;
        }
    }

    /**
     * Returns information about a user by their identifier. This is an offline request if the current user is not a bot.
     *
     * <p> Returns {@link User User} </p>
     */
    public static class GetUser extends Function {
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public GetUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         */
        public GetUser(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -47586017;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -47586017;
        }
    }

    /**
     * Returns full information about a user by their identifier.
     *
     * <p> Returns {@link UserFullInfo UserFullInfo} </p>
     */
    public static class GetUserFullInfo extends Function {
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public GetUserFullInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         */
        public GetUserFullInfo(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -655443263;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -655443263;
        }
    }

    /**
     * Returns the current privacy settings.
     *
     * <p> Returns {@link UserPrivacySettingRules UserPrivacySettingRules} </p>
     */
    public static class GetUserPrivacySettingRules extends Function {
        /**
         * The privacy setting.
         */
        public UserPrivacySetting setting;

        /**
         * Default constructor.
         */
        public GetUserPrivacySettingRules() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setting The privacy setting.
         */
        public GetUserPrivacySettingRules(UserPrivacySetting setting) {
            this.setting = setting;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2077223311;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2077223311;
        }
    }

    /**
     * Returns the profile photos of a user. The result of this query may be outdated: some photos might have been deleted already.
     *
     * <p> Returns {@link UserProfilePhotos UserProfilePhotos} </p>
     */
    public static class GetUserProfilePhotos extends Function {
        /**
         * User identifier.
         */
        public int userId;
        /**
         * The number of photos to skip; must be non-negative.
         */
        public int offset;
        /**
         * Maximum number of photos to be returned; up to 100.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public GetUserProfilePhotos() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         * @param offset The number of photos to skip; must be non-negative.
         * @param limit Maximum number of photos to be returned; up to 100.
         */
        public GetUserProfilePhotos(int userId, int offset, int limit) {
            this.userId = userId;
            this.offset = offset;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2062927433;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2062927433;
        }
    }

    /**
     * Returns background wallpapers.
     *
     * <p> Returns {@link Wallpapers Wallpapers} </p>
     */
    public static class GetWallpapers extends Function {

        /**
         * Default constructor.
         */
        public GetWallpapers() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2097518555;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2097518555;
        }
    }

    /**
     * Returns an instant view version of a web page if available. Returns a 404 error if the web page has no instant view page.
     *
     * <p> Returns {@link WebPageInstantView WebPageInstantView} </p>
     */
    public static class GetWebPageInstantView extends Function {
        /**
         * The web page URL.
         */
        public String url;
        /**
         * If true, the full instant view for the web page will be returned.
         */
        public boolean forceFull;

        /**
         * Default constructor.
         */
        public GetWebPageInstantView() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param url The web page URL.
         * @param forceFull If true, the full instant view for the web page will be returned.
         */
        public GetWebPageInstantView(String url, boolean forceFull) {
            this.url = url;
            this.forceFull = forceFull;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1962649975;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1962649975;
        }
    }

    /**
     * Returns a web page preview by the text of the message. Do not call this function too often. Returns a 404 error if the web page has no preview.
     *
     * <p> Returns {@link WebPage WebPage} </p>
     */
    public static class GetWebPagePreview extends Function {
        /**
         * Message text with formatting.
         */
        public FormattedText text;

        /**
         * Default constructor.
         */
        public GetWebPagePreview() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text Message text with formatting.
         */
        public GetWebPagePreview(FormattedText text) {
            this.text = text;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 573441580;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 573441580;
        }
    }

    /**
     * Adds new contacts or edits existing contacts; contacts' user identifiers are ignored.
     *
     * <p> Returns {@link ImportedContacts ImportedContacts} </p>
     */
    public static class ImportContacts extends Function {
        /**
         * The list of contacts to import or edit.
         */
        public Contact[] contacts;

        /**
         * Default constructor.
         */
        public ImportContacts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param contacts The list of contacts to import or edit.
         */
        public ImportContacts(Contact[] contacts) {
            this.contacts = contacts;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -215132767;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -215132767;
        }
    }

    /**
     * Uses an invite link to add the current user to the chat if possible. The new member will not be added until the chat state has been synchronized with the server.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class JoinChatByInviteLink extends Function {
        /**
         * Invite link to import; should begin with &quot;https://t.me/joinchat/&quot;, &quot;https://telegram.me/joinchat/&quot;, or &quot;https://telegram.dog/joinchat/&quot;.
         */
        public String inviteLink;

        /**
         * Default constructor.
         */
        public JoinChatByInviteLink() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inviteLink Invite link to import; should begin with &quot;https://t.me/joinchat/&quot;, &quot;https://telegram.me/joinchat/&quot;, or &quot;https://telegram.dog/joinchat/&quot;.
         */
        public JoinChatByInviteLink(String inviteLink) {
            this.inviteLink = inviteLink;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1049973882;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1049973882;
        }
    }

    /**
     * Closes the TDLib instance after a proper logout. Requires an available network connection. All local data will be destroyed. After the logout completes, updateAuthorizationState with authorizationStateClosed will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class LogOut extends Function {

        /**
         * Default constructor.
         */
        public LogOut() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1581923301;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1581923301;
        }
    }

    /**
     * This method should be called if the chat is opened by the user. Many useful activities depend on the chat being opened or closed (e.g., in supergroups and channels all updates are received only for opened chats).
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class OpenChat extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public OpenChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public OpenChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -323371509;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -323371509;
        }
    }

    /**
     * This method should be called if the message content has been opened (e.g., the user has opened a photo, video, document, location or venue, or has listened to an audio file or voice note message). An updateMessageContentOpened update will be generated if something has changed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class OpenMessageContent extends Function {
        /**
         * Chat identifier of the message.
         */
        public long chatId;
        /**
         * Identifier of the message with the opened content.
         */
        public long messageId;

        /**
         * Default constructor.
         */
        public OpenMessageContent() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the message.
         * @param messageId Identifier of the message with the opened content.
         */
        public OpenMessageContent(long chatId, long messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -739088005;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -739088005;
        }
    }

    /**
     * Optimizes storage usage, i.e. deletes some files and returns new storage usage statistics. Secret thumbnails can't be deleted.
     *
     * <p> Returns {@link StorageStatistics StorageStatistics} </p>
     */
    public static class OptimizeStorage extends Function {
        /**
         * Limit on the total size of files after deletion. Pass -1 to use the default limit.
         */
        public long size;
        /**
         * Limit on the time that has passed since the last time a file was accessed (or creation time for some filesystems). Pass -1 to use the default limit.
         */
        public int ttl;
        /**
         * Limit on the total count of files after deletion. Pass -1 to use the default limit.
         */
        public int count;
        /**
         * The amount of time after the creation of a file during which it can't be deleted, in seconds. Pass -1 to use the default value.
         */
        public int immunityDelay;
        /**
         * If not empty, only files with the given type(s) are considered. By default, all types except thumbnails, profile photos, stickers and wallpapers are deleted.
         */
        public FileType[] fileTypes;
        /**
         * If not empty, only files from the given chats are considered. Use 0 as chat identifier to delete files not belonging to any chat (e.g., profile photos).
         */
        public long[] chatIds;
        /**
         * If not empty, files from the given chats are excluded. Use 0 as chat identifier to exclude all files not belonging to any chat (e.g., profile photos).
         */
        public long[] excludeChatIds;
        /**
         * Same as in getStorageStatistics. Affects only returned statistics.
         */
        public int chatLimit;

        /**
         * Default constructor.
         */
        public OptimizeStorage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param size Limit on the total size of files after deletion. Pass -1 to use the default limit.
         * @param ttl Limit on the time that has passed since the last time a file was accessed (or creation time for some filesystems). Pass -1 to use the default limit.
         * @param count Limit on the total count of files after deletion. Pass -1 to use the default limit.
         * @param immunityDelay The amount of time after the creation of a file during which it can't be deleted, in seconds. Pass -1 to use the default value.
         * @param fileTypes If not empty, only files with the given type(s) are considered. By default, all types except thumbnails, profile photos, stickers and wallpapers are deleted.
         * @param chatIds If not empty, only files from the given chats are considered. Use 0 as chat identifier to delete files not belonging to any chat (e.g., profile photos).
         * @param excludeChatIds If not empty, files from the given chats are excluded. Use 0 as chat identifier to exclude all files not belonging to any chat (e.g., profile photos).
         * @param chatLimit Same as in getStorageStatistics. Affects only returned statistics.
         */
        public OptimizeStorage(long size, int ttl, int count, int immunityDelay, FileType[] fileTypes, long[] chatIds, long[] excludeChatIds, int chatLimit) {
            this.size = size;
            this.ttl = ttl;
            this.count = count;
            this.immunityDelay = immunityDelay;
            this.fileTypes = fileTypes;
            this.chatIds = chatIds;
            this.excludeChatIds = excludeChatIds;
            this.chatLimit = chatLimit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 980397489;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 980397489;
        }
    }

    /**
     * Parses Bold, Italic, Code, Pre, PreCode and TextUrl entities contained in the text. This is an offline method. Can be called before authorization. Can be called synchronously.
     *
     * <p> Returns {@link FormattedText FormattedText} </p>
     */
    public static class ParseTextEntities extends Function {
        /**
         * The text which should be parsed.
         */
        public String text;
        /**
         * Text parse mode.
         */
        public TextParseMode parseMode;

        /**
         * Default constructor.
         */
        public ParseTextEntities() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param text The text which should be parsed.
         * @param parseMode Text parse mode.
         */
        public ParseTextEntities(String text, TextParseMode parseMode) {
            this.text = text;
            this.parseMode = parseMode;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1709194593;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1709194593;
        }
    }

    /**
     * Pins a message in a supergroup or channel; requires appropriate administrator rights in the supergroup or channel.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class PinSupergroupMessage extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;
        /**
         * Identifier of the new pinned message.
         */
        public long messageId;
        /**
         * True, if there should be no notification about the pinned message.
         */
        public boolean disableNotification;

        /**
         * Default constructor.
         */
        public PinSupergroupMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         * @param messageId Identifier of the new pinned message.
         * @param disableNotification True, if there should be no notification about the pinned message.
         */
        public PinSupergroupMessage(int supergroupId, long messageId, boolean disableNotification) {
            this.supergroupId = supergroupId;
            this.messageId = messageId;
            this.disableNotification = disableNotification;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1141187557;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1141187557;
        }
    }

    /**
     * Handles a DCUPDATE push service notification. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ProcessDcUpdate extends Function {
        /**
         * Value of the &quot;dc&quot; parameter of the notification.
         */
        public String dc;
        /**
         * Value of the &quot;addr&quot; parameter of the notification.
         */
        public String addr;

        /**
         * Default constructor.
         */
        public ProcessDcUpdate() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param dc Value of the &quot;dc&quot; parameter of the notification.
         * @param addr Value of the &quot;addr&quot; parameter of the notification.
         */
        public ProcessDcUpdate(String dc, String addr) {
            this.dc = dc;
            this.addr = addr;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1806562997;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1806562997;
        }
    }

    /**
     * Marks all mentions in a chat as read.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ReadAllChatMentions extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public ReadAllChatMentions() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public ReadAllChatMentions(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1357558453;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1357558453;
        }
    }

    /**
     * Recovers the password with a password recovery code sent to an email address that was previously set up. Works only when the current authorization state is authorizationStateWaitPassword.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RecoverAuthenticationPassword extends Function {
        /**
         * Recovery code to check.
         */
        public String recoveryCode;

        /**
         * Default constructor.
         */
        public RecoverAuthenticationPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param recoveryCode Recovery code to check.
         */
        public RecoverAuthenticationPassword(String recoveryCode) {
            this.recoveryCode = recoveryCode;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 787436412;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 787436412;
        }
    }

    /**
     * Recovers the password using a recovery code sent to an email address that was previously set up.
     *
     * <p> Returns {@link PasswordState PasswordState} </p>
     */
    public static class RecoverPassword extends Function {
        /**
         * Recovery code to check.
         */
        public String recoveryCode;

        /**
         * Default constructor.
         */
        public RecoverPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param recoveryCode Recovery code to check.
         */
        public RecoverPassword(String recoveryCode) {
            this.recoveryCode = recoveryCode;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1660185903;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1660185903;
        }
    }

    /**
     * Registers the currently used device for receiving push notifications.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RegisterDevice extends Function {
        /**
         * Device token.
         */
        public DeviceToken deviceToken;
        /**
         * List of at most 100 user identifiers of other users currently using the client.
         */
        public int[] otherUserIds;

        /**
         * Default constructor.
         */
        public RegisterDevice() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param deviceToken Device token.
         * @param otherUserIds List of at most 100 user identifiers of other users currently using the client.
         */
        public RegisterDevice(DeviceToken deviceToken, int[] otherUserIds) {
            this.deviceToken = deviceToken;
            this.otherUserIds = otherUserIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -413637293;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -413637293;
        }
    }

    /**
     * Removes users from the contacts list.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveContacts extends Function {
        /**
         * Identifiers of users to be deleted.
         */
        public int[] userIds;

        /**
         * Default constructor.
         */
        public RemoveContacts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userIds Identifiers of users to be deleted.
         */
        public RemoveContacts(int[] userIds) {
            this.userIds = userIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -615510759;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -615510759;
        }
    }

    /**
     * Removes a sticker from the list of favorite stickers.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveFavoriteSticker extends Function {
        /**
         * Sticker file to delete from the list.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public RemoveFavoriteSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker file to delete from the list.
         */
        public RemoveFavoriteSticker(InputFile sticker) {
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1152945264;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1152945264;
        }
    }

    /**
     * Removes a hashtag from the list of recently used hashtags.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveRecentHashtag extends Function {
        /**
         * Hashtag to delete.
         */
        public String hashtag;

        /**
         * Default constructor.
         */
        public RemoveRecentHashtag() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param hashtag Hashtag to delete.
         */
        public RemoveRecentHashtag(String hashtag) {
            this.hashtag = hashtag;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1013735260;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1013735260;
        }
    }

    /**
     * Removes a sticker from the list of recently used stickers.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveRecentSticker extends Function {
        /**
         * Pass true to remove the sticker from the list of stickers recently attached to photo or video files; pass false to remove the sticker from the list of recently sent stickers.
         */
        public boolean isAttached;
        /**
         * Sticker file to delete.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public RemoveRecentSticker() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isAttached Pass true to remove the sticker from the list of stickers recently attached to photo or video files; pass false to remove the sticker from the list of recently sent stickers.
         * @param sticker Sticker file to delete.
         */
        public RemoveRecentSticker(boolean isAttached, InputFile sticker) {
            this.isAttached = isAttached;
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1246577677;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1246577677;
        }
    }

    /**
     * Removes a chat from the list of recently found chats.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveRecentlyFoundChat extends Function {
        /**
         * Identifier of the chat to be removed.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public RemoveRecentlyFoundChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat to be removed.
         */
        public RemoveRecentlyFoundChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 717340444;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 717340444;
        }
    }

    /**
     * Removes an animation from the list of saved animations.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveSavedAnimation extends Function {
        /**
         * Animation file to be removed.
         */
        public InputFile animation;

        /**
         * Default constructor.
         */
        public RemoveSavedAnimation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param animation Animation file to be removed.
         */
        public RemoveSavedAnimation(InputFile animation) {
            this.animation = animation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -495605479;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -495605479;
        }
    }

    /**
     * Removes a sticker from the set to which it belongs; for bots only. The sticker set must have been created by the bot.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveStickerFromSet extends Function {
        /**
         * Sticker.
         */
        public InputFile sticker;

        /**
         * Default constructor.
         */
        public RemoveStickerFromSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker.
         */
        public RemoveStickerFromSet(InputFile sticker) {
            this.sticker = sticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1642196644;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1642196644;
        }
    }

    /**
     * Removes a chat from the list of frequently used chats. Supported only if the chat info database is enabled.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RemoveTopChat extends Function {
        /**
         * Category of frequently used chats.
         */
        public TopChatCategory category;
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public RemoveTopChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param category Category of frequently used chats.
         * @param chatId Chat identifier.
         */
        public RemoveTopChat(TopChatCategory category, long chatId) {
            this.category = category;
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1907876267;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1907876267;
        }
    }

    /**
     * Changes the order of installed sticker sets.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ReorderInstalledStickerSets extends Function {
        /**
         * Pass true to change the order of mask sticker sets; pass false to change the order of ordinary sticker sets.
         */
        public boolean isMasks;
        /**
         * Identifiers of installed sticker sets in the new correct order.
         */
        public long[] stickerSetIds;

        /**
         * Default constructor.
         */
        public ReorderInstalledStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMasks Pass true to change the order of mask sticker sets; pass false to change the order of ordinary sticker sets.
         * @param stickerSetIds Identifiers of installed sticker sets in the new correct order.
         */
        public ReorderInstalledStickerSets(boolean isMasks, long[] stickerSetIds) {
            this.isMasks = isMasks;
            this.stickerSetIds = stickerSetIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1114537563;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1114537563;
        }
    }

    /**
     * Reports a chat to the Telegram moderators. Supported only for supergroups, channels, or private chats with bots, since other chats can't be checked by moderators.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ReportChat extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The reason for reporting the chat.
         */
        public ChatReportReason reason;
        /**
         * Identifiers of reported messages, if any.
         */
        public long[] messageIds;

        /**
         * Default constructor.
         */
        public ReportChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param reason The reason for reporting the chat.
         * @param messageIds Identifiers of reported messages, if any.
         */
        public ReportChat(long chatId, ChatReportReason reason, long[] messageIds) {
            this.chatId = chatId;
            this.reason = reason;
            this.messageIds = messageIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -312579772;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -312579772;
        }
    }

    /**
     * Reports some messages from a user in a supergroup as spam.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ReportSupergroupSpam extends Function {
        /**
         * Supergroup identifier.
         */
        public int supergroupId;
        /**
         * User identifier.
         */
        public int userId;
        /**
         * Identifiers of messages sent in the supergroup by the user. This list must be non-empty.
         */
        public long[] messageIds;

        /**
         * Default constructor.
         */
        public ReportSupergroupSpam() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Supergroup identifier.
         * @param userId User identifier.
         * @param messageIds Identifiers of messages sent in the supergroup by the user. This list must be non-empty.
         */
        public ReportSupergroupSpam(int supergroupId, int userId, long[] messageIds) {
            this.supergroupId = supergroupId;
            this.userId = userId;
            this.messageIds = messageIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2125451498;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2125451498;
        }
    }

    /**
     * Requests to send a password recovery code to an email address that was previously set up. Works only when the current authorization state is authorizationStateWaitPassword.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class RequestAuthenticationPasswordRecovery extends Function {

        /**
         * Default constructor.
         */
        public RequestAuthenticationPasswordRecovery() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1393896118;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1393896118;
        }
    }

    /**
     * Requests to send a password recovery code to an email address that was previously set up.
     *
     * <p> Returns {@link PasswordRecoveryInfo PasswordRecoveryInfo} </p>
     */
    public static class RequestPasswordRecovery extends Function {

        /**
         * Default constructor.
         */
        public RequestPasswordRecovery() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1989252384;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1989252384;
        }
    }

    /**
     * Re-sends an authentication code to the user. Works only when the current authorization state is authorizationStateWaitCode and the nextCodeType of the result is not null.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ResendAuthenticationCode extends Function {

        /**
         * Default constructor.
         */
        public ResendAuthenticationCode() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -814377191;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -814377191;
        }
    }

    /**
     * Re-sends the authentication code sent to confirm a new phone number for the user. Works only if the previously received authenticationCodeInfo nextCodeType was not null.
     *
     * <p> Returns {@link AuthenticationCodeInfo AuthenticationCodeInfo} </p>
     */
    public static class ResendChangePhoneNumberCode extends Function {

        /**
         * Default constructor.
         */
        public ResendChangePhoneNumberCode() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -786772060;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -786772060;
        }
    }

    /**
     * Resets all notification settings to their default values. By default, the only muted chats are supergroups, the sound is set to &quot;default&quot; and message previews are shown.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ResetAllNotificationSettings extends Function {

        /**
         * Default constructor.
         */
        public ResetAllNotificationSettings() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -174020359;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -174020359;
        }
    }

    /**
     * Resets all network data usage statistics to zero. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ResetNetworkStatistics extends Function {

        /**
         * Default constructor.
         */
        public ResetNetworkStatistics() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1646452102;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1646452102;
        }
    }

    /**
     * Searches for call messages. Returns the results in reverse chronological order (i. e., in order of decreasing messageId). For optimal performance the number of returned messages is chosen by the library.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class SearchCallMessages extends Function {
        /**
         * Identifier of the message from which to search; use 0 to get results from the beginning.
         */
        public long fromMessageId;
        /**
         * The maximum number of messages to be returned; up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public int limit;
        /**
         * If true, returns only messages with missed calls.
         */
        public boolean onlyMissed;

        /**
         * Default constructor.
         */
        public SearchCallMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param fromMessageId Identifier of the message from which to search; use 0 to get results from the beginning.
         * @param limit The maximum number of messages to be returned; up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         * @param onlyMissed If true, returns only messages with missed calls.
         */
        public SearchCallMessages(long fromMessageId, int limit, boolean onlyMissed) {
            this.fromMessageId = fromMessageId;
            this.limit = limit;
            this.onlyMissed = onlyMissed;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1077230820;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1077230820;
        }
    }

    /**
     * Searches for a specified query in the first name, last name and username of the members of a specified chat. Requires administrator rights in channels.
     *
     * <p> Returns {@link ChatMembers ChatMembers} </p>
     */
    public static class SearchChatMembers extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Query to search for.
         */
        public String query;
        /**
         * The maximum number of users to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchChatMembers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param query Query to search for.
         * @param limit The maximum number of users to be returned.
         */
        public SearchChatMembers(long chatId, String query, int limit) {
            this.chatId = chatId;
            this.query = query;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1538035890;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1538035890;
        }
    }

    /**
     * Searches for messages with given words in the chat. Returns the results in reverse chronological order, i.e. in order of decreasing messageId. Cannot be used in secret chats with a non-empty query (searchSecretMessages should be used instead), or without an enabled message database. For optimal performance the number of returned messages is chosen by the library.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class SearchChatMessages extends Function {
        /**
         * Identifier of the chat in which to search messages.
         */
        public long chatId;
        /**
         * Query to search for.
         */
        public String query;
        /**
         * If not 0, only messages sent by the specified user will be returned. Not supported in secret chats.
         */
        public int senderUserId;
        /**
         * Identifier of the message starting from which history must be fetched; use 0 to get results from the beginning.
         */
        public long fromMessageId;
        /**
         * Specify 0 to get results from exactly the fromMessageId or a negative offset to get the specified message and some newer messages.
         */
        public int offset;
        /**
         * The maximum number of messages to be returned; must be positive and can't be greater than 100. If the offset is negative, the limit must be greater than -offset. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public int limit;
        /**
         * Filter for message content in the search results.
         */
        public SearchMessagesFilter filter;

        /**
         * Default constructor.
         */
        public SearchChatMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat in which to search messages.
         * @param query Query to search for.
         * @param senderUserId If not 0, only messages sent by the specified user will be returned. Not supported in secret chats.
         * @param fromMessageId Identifier of the message starting from which history must be fetched; use 0 to get results from the beginning.
         * @param offset Specify 0 to get results from exactly the fromMessageId or a negative offset to get the specified message and some newer messages.
         * @param limit The maximum number of messages to be returned; must be positive and can't be greater than 100. If the offset is negative, the limit must be greater than -offset. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         * @param filter Filter for message content in the search results.
         */
        public SearchChatMessages(long chatId, String query, int senderUserId, long fromMessageId, int offset, int limit, SearchMessagesFilter filter) {
            this.chatId = chatId;
            this.query = query;
            this.senderUserId = senderUserId;
            this.fromMessageId = fromMessageId;
            this.offset = offset;
            this.limit = limit;
            this.filter = filter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1528846671;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1528846671;
        }
    }

    /**
     * Returns information about the recent locations of chat members that were sent to the chat. Returns up to 1 location message per user.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class SearchChatRecentLocationMessages extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * Maximum number of messages to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchChatRecentLocationMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param limit Maximum number of messages to be returned.
         */
        public SearchChatRecentLocationMessages(long chatId, int limit) {
            this.chatId = chatId;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 950238950;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 950238950;
        }
    }

    /**
     * Searches for the specified query in the title and username of already known chats, this is an offline request. Returns chats in the order seen in the chat list.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class SearchChats extends Function {
        /**
         * Query to search for. If the query is empty, returns up to 20 recently found chats.
         */
        public String query;
        /**
         * Maximum number of chats to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchChats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for. If the query is empty, returns up to 20 recently found chats.
         * @param limit Maximum number of chats to be returned.
         */
        public SearchChats(String query, int limit) {
            this.query = query;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1879787060;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1879787060;
        }
    }

    /**
     * Searches for the specified query in the title and username of already known chats via request to the server. Returns chats in the order seen in the chat list.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class SearchChatsOnServer extends Function {
        /**
         * Query to search for.
         */
        public String query;
        /**
         * Maximum number of chats to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchChatsOnServer() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         * @param limit Maximum number of chats to be returned.
         */
        public SearchChatsOnServer(String query, int limit) {
            this.query = query;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1158402188;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1158402188;
        }
    }

    /**
     * Searches for the specified query in the first names, last names and usernames of the known user contacts.
     *
     * <p> Returns {@link Users Users} </p>
     */
    public static class SearchContacts extends Function {
        /**
         * Query to search for; can be empty to return all contacts.
         */
        public String query;
        /**
         * Maximum number of users to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchContacts() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for; can be empty to return all contacts.
         * @param limit Maximum number of users to be returned.
         */
        public SearchContacts(String query, int limit) {
            this.query = query;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1794690715;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1794690715;
        }
    }

    /**
     * Searches for recently used hashtags by their prefix.
     *
     * <p> Returns {@link Hashtags Hashtags} </p>
     */
    public static class SearchHashtags extends Function {
        /**
         * Hashtag prefix to search for.
         */
        public String prefix;
        /**
         * Maximum number of hashtags to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchHashtags() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param prefix Hashtag prefix to search for.
         * @param limit Maximum number of hashtags to be returned.
         */
        public SearchHashtags(String prefix, int limit) {
            this.prefix = prefix;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1043637617;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1043637617;
        }
    }

    /**
     * Searches for installed sticker sets by looking for specified query in their title and name.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class SearchInstalledStickerSets extends Function {
        /**
         * Pass true to return mask sticker sets; pass false to return ordinary sticker sets.
         */
        public boolean isMasks;
        /**
         * Query to search for.
         */
        public String query;
        /**
         * Maximum number of sticker sets to return.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchInstalledStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param isMasks Pass true to return mask sticker sets; pass false to return ordinary sticker sets.
         * @param query Query to search for.
         * @param limit Maximum number of sticker sets to return.
         */
        public SearchInstalledStickerSets(boolean isMasks, String query, int limit) {
            this.isMasks = isMasks;
            this.query = query;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 681171344;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 681171344;
        }
    }

    /**
     * Searches for messages in all chats except secret chats. Returns the results in reverse chronological order (i.e., in order of decreasing (date, chatId, messageId)). For optimal performance the number of returned messages is chosen by the library.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class SearchMessages extends Function {
        /**
         * Query to search for.
         */
        public String query;
        /**
         * The date of the message starting from which the results should be fetched. Use 0 or any date in the future to get results from the beginning.
         */
        public int offsetDate;
        /**
         * The chat identifier of the last found message, or 0 for the first request.
         */
        public long offsetChatId;
        /**
         * The message identifier of the last found message, or 0 for the first request.
         */
        public long offsetMessageId;
        /**
         * The maximum number of messages to be returned, up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         * @param offsetDate The date of the message starting from which the results should be fetched. Use 0 or any date in the future to get results from the beginning.
         * @param offsetChatId The chat identifier of the last found message, or 0 for the first request.
         * @param offsetMessageId The message identifier of the last found message, or 0 for the first request.
         * @param limit The maximum number of messages to be returned, up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public SearchMessages(String query, int offsetDate, long offsetChatId, long offsetMessageId, int limit) {
            this.query = query;
            this.offsetDate = offsetDate;
            this.offsetChatId = offsetChatId;
            this.offsetMessageId = offsetMessageId;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1579305146;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1579305146;
        }
    }

    /**
     * Searches a public chat by its username. Currently only private chats, supergroups and channels can be public. Returns the chat if found; otherwise an error is returned.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class SearchPublicChat extends Function {
        /**
         * Username to be resolved.
         */
        public String username;

        /**
         * Default constructor.
         */
        public SearchPublicChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param username Username to be resolved.
         */
        public SearchPublicChat(String username) {
            this.username = username;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 857135533;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 857135533;
        }
    }

    /**
     * Searches public chats by looking for specified query in their username and title. Currently only private chats, supergroups and channels can be public. Returns a meaningful number of results. Returns nothing if the length of the searched username prefix is less than 5. Excludes private chats with contacts and chats from the chat list from the results.
     *
     * <p> Returns {@link Chats Chats} </p>
     */
    public static class SearchPublicChats extends Function {
        /**
         * Query to search for.
         */
        public String query;

        /**
         * Default constructor.
         */
        public SearchPublicChats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         */
        public SearchPublicChats(String query) {
            this.query = query;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 970385337;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 970385337;
        }
    }

    /**
     * Searches for messages in secret chats. Returns the results in reverse chronological order. For optimal performance the number of returned messages is chosen by the library.
     *
     * <p> Returns {@link FoundMessages FoundMessages} </p>
     */
    public static class SearchSecretMessages extends Function {
        /**
         * Identifier of the chat in which to search. Specify 0 to search in all secret chats.
         */
        public long chatId;
        /**
         * Query to search for. If empty, searchChatMessages should be used instead.
         */
        public String query;
        /**
         * The identifier from the result of a previous request, use 0 to get results from the beginning.
         */
        public long fromSearchId;
        /**
         * Maximum number of messages to be returned; up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         */
        public int limit;
        /**
         * A filter for the content of messages in the search results.
         */
        public SearchMessagesFilter filter;

        /**
         * Default constructor.
         */
        public SearchSecretMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat in which to search. Specify 0 to search in all secret chats.
         * @param query Query to search for. If empty, searchChatMessages should be used instead.
         * @param fromSearchId The identifier from the result of a previous request, use 0 to get results from the beginning.
         * @param limit Maximum number of messages to be returned; up to 100. Fewer messages may be returned than specified by the limit, even if the end of the message history has not been reached.
         * @param filter A filter for the content of messages in the search results.
         */
        public SearchSecretMessages(long chatId, String query, long fromSearchId, int limit, SearchMessagesFilter filter) {
            this.chatId = chatId;
            this.query = query;
            this.fromSearchId = fromSearchId;
            this.limit = limit;
            this.filter = filter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1670627915;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1670627915;
        }
    }

    /**
     * Searches for a sticker set by its name.
     *
     * <p> Returns {@link StickerSet StickerSet} </p>
     */
    public static class SearchStickerSet extends Function {
        /**
         * Name of the sticker set.
         */
        public String name;

        /**
         * Default constructor.
         */
        public SearchStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name Name of the sticker set.
         */
        public SearchStickerSet(String name) {
            this.name = name;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1157930222;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1157930222;
        }
    }

    /**
     * Searches for ordinary sticker sets by looking for specified query in their title and name. Excludes installed sticker sets from the results.
     *
     * <p> Returns {@link StickerSets StickerSets} </p>
     */
    public static class SearchStickerSets extends Function {
        /**
         * Query to search for.
         */
        public String query;

        /**
         * Default constructor.
         */
        public SearchStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param query Query to search for.
         */
        public SearchStickerSets(String query) {
            this.query = query;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1082314629;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1082314629;
        }
    }

    /**
     * Searches for stickers from public sticker sets that correspond to a given emoji.
     *
     * <p> Returns {@link Stickers Stickers} </p>
     */
    public static class SearchStickers extends Function {
        /**
         * String representation of emoji; must be non-empty.
         */
        public String emoji;
        /**
         * Maximum number of stickers to be returned.
         */
        public int limit;

        /**
         * Default constructor.
         */
        public SearchStickers() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param emoji String representation of emoji; must be non-empty.
         * @param limit Maximum number of stickers to be returned.
         */
        public SearchStickers(String emoji, int limit) {
            this.emoji = emoji;
            this.limit = limit;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1555771203;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1555771203;
        }
    }

    /**
     * Invites a bot to a chat (if it is not yet a member) and sends it the /start command. Bots can't be invited to a private chat other than the chat with the bot. Bots can't be invited to channels (although they can be added as admins) and secret chats. Returns the sent message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class SendBotStartMessage extends Function {
        /**
         * Identifier of the bot.
         */
        public int botUserId;
        /**
         * Identifier of the target chat.
         */
        public long chatId;
        /**
         * A hidden parameter sent to the bot for deep linking purposes (https://api.telegram.org/bots#deep-linking).
         */
        public String parameter;

        /**
         * Default constructor.
         */
        public SendBotStartMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param botUserId Identifier of the bot.
         * @param chatId Identifier of the target chat.
         * @param parameter A hidden parameter sent to the bot for deep linking purposes (https://api.telegram.org/bots#deep-linking).
         */
        public SendBotStartMessage(int botUserId, long chatId, String parameter) {
            this.botUserId = botUserId;
            this.chatId = chatId;
            this.parameter = parameter;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1112181339;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1112181339;
        }
    }

    /**
     * Sends debug information for a call.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SendCallDebugInformation extends Function {
        /**
         * Call identifier.
         */
        public int callId;
        /**
         * Debug information in application-specific format.
         */
        public String debugInformation;

        /**
         * Default constructor.
         */
        public SendCallDebugInformation() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param callId Call identifier.
         * @param debugInformation Debug information in application-specific format.
         */
        public SendCallDebugInformation(int callId, String debugInformation) {
            this.callId = callId;
            this.debugInformation = debugInformation;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2019243839;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2019243839;
        }
    }

    /**
     * Sends a call rating.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SendCallRating extends Function {
        /**
         * Call identifier.
         */
        public int callId;
        /**
         * Call rating; 1-5.
         */
        public int rating;
        /**
         * An optional user comment if the rating is less than 5.
         */
        public String comment;

        /**
         * Default constructor.
         */
        public SendCallRating() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param callId Call identifier.
         * @param rating Call rating; 1-5.
         * @param comment An optional user comment if the rating is less than 5.
         */
        public SendCallRating(int callId, int rating, String comment) {
            this.callId = callId;
            this.rating = rating;
            this.comment = comment;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 243075146;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 243075146;
        }
    }

    /**
     * Sends a notification about user activity in a chat.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SendChatAction extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The action description.
         */
        public ChatAction action;

        /**
         * Default constructor.
         */
        public SendChatAction() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param action The action description.
         */
        public SendChatAction(long chatId, ChatAction action) {
            this.chatId = chatId;
            this.action = action;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -841357536;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -841357536;
        }
    }

    /**
     * Sends a notification about a screenshot taken in a chat. Supported only in private and secret chats.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SendChatScreenshotTakenNotification extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public SendChatScreenshotTakenNotification() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         */
        public SendChatScreenshotTakenNotification(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 448399457;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 448399457;
        }
    }

    /**
     * Changes the current TTL setting (sets a new self-destruct timer) in a secret chat and sends the corresponding message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class SendChatSetTtlMessage extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New TTL value, in seconds.
         */
        public int ttl;

        /**
         * Default constructor.
         */
        public SendChatSetTtlMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param ttl New TTL value, in seconds.
         */
        public SendChatSetTtlMessage(long chatId, int ttl) {
            this.chatId = chatId;
            this.ttl = ttl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1432535564;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1432535564;
        }
    }

    /**
     * Sends a custom request; for bots only.
     *
     * <p> Returns {@link CustomRequestResult CustomRequestResult} </p>
     */
    public static class SendCustomRequest extends Function {
        /**
         * The method name.
         */
        public String method;
        /**
         * JSON-serialized method parameters.
         */
        public String parameters;

        /**
         * Default constructor.
         */
        public SendCustomRequest() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param method The method name.
         * @param parameters JSON-serialized method parameters.
         */
        public SendCustomRequest(String method, String parameters) {
            this.method = method;
            this.parameters = parameters;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 285045153;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 285045153;
        }
    }

    /**
     * Sends the result of an inline query as a message. Returns the sent message. Always clears a chat draft message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class SendInlineQueryResultMessage extends Function {
        /**
         * Target chat.
         */
        public long chatId;
        /**
         * Identifier of a message to reply to or 0.
         */
        public long replyToMessageId;
        /**
         * Pass true to disable notification for the message. Not supported in secret chats.
         */
        public boolean disableNotification;
        /**
         * Pass true if the message is sent from background.
         */
        public boolean fromBackground;
        /**
         * Identifier of the inline query.
         */
        public long queryId;
        /**
         * Identifier of the inline result.
         */
        public String resultId;

        /**
         * Default constructor.
         */
        public SendInlineQueryResultMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Target chat.
         * @param replyToMessageId Identifier of a message to reply to or 0.
         * @param disableNotification Pass true to disable notification for the message. Not supported in secret chats.
         * @param fromBackground Pass true if the message is sent from background.
         * @param queryId Identifier of the inline query.
         * @param resultId Identifier of the inline result.
         */
        public SendInlineQueryResultMessage(long chatId, long replyToMessageId, boolean disableNotification, boolean fromBackground, long queryId, String resultId) {
            this.chatId = chatId;
            this.replyToMessageId = replyToMessageId;
            this.disableNotification = disableNotification;
            this.fromBackground = fromBackground;
            this.queryId = queryId;
            this.resultId = resultId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -643910868;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -643910868;
        }
    }

    /**
     * Sends a message. Returns the sent message.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class SendMessage extends Function {
        /**
         * Target chat.
         */
        public long chatId;
        /**
         * Identifier of the message to reply to or 0.
         */
        public long replyToMessageId;
        /**
         * Pass true to disable notification for the message. Not supported in secret chats.
         */
        public boolean disableNotification;
        /**
         * Pass true if the message is sent from the background.
         */
        public boolean fromBackground;
        /**
         * Markup for replying to the message; for bots only.
         */
        public ReplyMarkup replyMarkup;
        /**
         * The content of the message to be sent.
         */
        public InputMessageContent inputMessageContent;

        /**
         * Default constructor.
         */
        public SendMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Target chat.
         * @param replyToMessageId Identifier of the message to reply to or 0.
         * @param disableNotification Pass true to disable notification for the message. Not supported in secret chats.
         * @param fromBackground Pass true if the message is sent from the background.
         * @param replyMarkup Markup for replying to the message; for bots only.
         * @param inputMessageContent The content of the message to be sent.
         */
        public SendMessage(long chatId, long replyToMessageId, boolean disableNotification, boolean fromBackground, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent) {
            this.chatId = chatId;
            this.replyToMessageId = replyToMessageId;
            this.disableNotification = disableNotification;
            this.fromBackground = fromBackground;
            this.replyMarkup = replyMarkup;
            this.inputMessageContent = inputMessageContent;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1694632114;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1694632114;
        }
    }

    /**
     * Sends messages grouped together into an album. Currently only photo and video messages can be grouped into an album. Returns sent messages.
     *
     * <p> Returns {@link Messages Messages} </p>
     */
    public static class SendMessageAlbum extends Function {
        /**
         * Target chat.
         */
        public long chatId;
        /**
         * Identifier of a message to reply to or 0.
         */
        public long replyToMessageId;
        /**
         * Pass true to disable notification for the messages. Not supported in secret chats.
         */
        public boolean disableNotification;
        /**
         * Pass true if the messages are sent from the background.
         */
        public boolean fromBackground;
        /**
         * Contents of messages to be sent.
         */
        public InputMessageContent[] inputMessageContents;

        /**
         * Default constructor.
         */
        public SendMessageAlbum() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Target chat.
         * @param replyToMessageId Identifier of a message to reply to or 0.
         * @param disableNotification Pass true to disable notification for the messages. Not supported in secret chats.
         * @param fromBackground Pass true if the messages are sent from the background.
         * @param inputMessageContents Contents of messages to be sent.
         */
        public SendMessageAlbum(long chatId, long replyToMessageId, boolean disableNotification, boolean fromBackground, InputMessageContent[] inputMessageContents) {
            this.chatId = chatId;
            this.replyToMessageId = replyToMessageId;
            this.disableNotification = disableNotification;
            this.fromBackground = fromBackground;
            this.inputMessageContents = inputMessageContents;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -291823014;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -291823014;
        }
    }

    /**
     * Sends a filled-out payment form to the bot for final verification.
     *
     * <p> Returns {@link PaymentResult PaymentResult} </p>
     */
    public static class SendPaymentForm extends Function {
        /**
         * Chat identifier of the Invoice message.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * Identifier returned by ValidateOrderInfo, or an empty string.
         */
        public String orderInfoId;
        /**
         * Identifier of a chosen shipping option, if applicable.
         */
        public String shippingOptionId;
        /**
         * The credentials chosen by user for payment.
         */
        public InputCredentials credentials;

        /**
         * Default constructor.
         */
        public SendPaymentForm() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the Invoice message.
         * @param messageId Message identifier.
         * @param orderInfoId Identifier returned by ValidateOrderInfo, or an empty string.
         * @param shippingOptionId Identifier of a chosen shipping option, if applicable.
         * @param credentials The credentials chosen by user for payment.
         */
        public SendPaymentForm(long chatId, long messageId, String orderInfoId, String shippingOptionId, InputCredentials credentials) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.orderInfoId = orderInfoId;
            this.shippingOptionId = shippingOptionId;
            this.credentials = credentials;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 591581572;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 591581572;
        }
    }

    /**
     * Changes the period of inactivity after which the account of the current user will automatically be deleted.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetAccountTtl extends Function {
        /**
         * New account TTL.
         */
        public AccountTtl ttl;

        /**
         * Default constructor.
         */
        public SetAccountTtl() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param ttl New account TTL.
         */
        public SetAccountTtl(AccountTtl ttl) {
            this.ttl = ttl;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 701389032;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 701389032;
        }
    }

    /**
     * Succeeds after a specified amount of time has passed. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetAlarm extends Function {
        /**
         * Number of seconds before the function returns.
         */
        public double seconds;

        /**
         * Default constructor.
         */
        public SetAlarm() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param seconds Number of seconds before the function returns.
         */
        public SetAlarm(double seconds) {
            this.seconds = seconds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -873497067;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -873497067;
        }
    }

    /**
     * Sets the phone number of the user and sends an authentication code to the user. Works only when the current authorization state is authorizationStateWaitPhoneNumber.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetAuthenticationPhoneNumber extends Function {
        /**
         * The phone number of the user, in international format.
         */
        public String phoneNumber;
        /**
         * Pass true if the authentication code may be sent via flash call to the specified phone number.
         */
        public boolean allowFlashCall;
        /**
         * Pass true if the phone number is used on the current device. Ignored if allowFlashCall is false.
         */
        public boolean isCurrentPhoneNumber;

        /**
         * Default constructor.
         */
        public SetAuthenticationPhoneNumber() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param phoneNumber The phone number of the user, in international format.
         * @param allowFlashCall Pass true if the authentication code may be sent via flash call to the specified phone number.
         * @param isCurrentPhoneNumber Pass true if the phone number is used on the current device. Ignored if allowFlashCall is false.
         */
        public SetAuthenticationPhoneNumber(String phoneNumber, boolean allowFlashCall, boolean isCurrentPhoneNumber) {
            this.phoneNumber = phoneNumber;
            this.allowFlashCall = allowFlashCall;
            this.isCurrentPhoneNumber = isCurrentPhoneNumber;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -856055465;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -856055465;
        }
    }

    /**
     * Changes the bio of the current user.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetBio extends Function {
        /**
         * The new value of the user bio; 0-70 characters without line feeds.
         */
        public String bio;

        /**
         * Default constructor.
         */
        public SetBio() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param bio The new value of the user bio; 0-70 characters without line feeds.
         */
        public SetBio(String bio) {
            this.bio = bio;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1619582124;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1619582124;
        }
    }

    /**
     * Informs the server about the number of pending bot updates if they haven't been processed for a long time; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetBotUpdatesStatus extends Function {
        /**
         * The number of pending updates.
         */
        public int pendingUpdateCount;
        /**
         * The last error message.
         */
        public String errorMessage;

        /**
         * Default constructor.
         */
        public SetBotUpdatesStatus() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param pendingUpdateCount The number of pending updates.
         * @param errorMessage The last error message.
         */
        public SetBotUpdatesStatus(int pendingUpdateCount, String errorMessage) {
            this.pendingUpdateCount = pendingUpdateCount;
            this.errorMessage = errorMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1154926191;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1154926191;
        }
    }

    /**
     * Changes client data associated with a chat.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetChatClientData extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New value of clientData.
         */
        public String clientData;

        /**
         * Default constructor.
         */
        public SetChatClientData() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param clientData New value of clientData.
         */
        public SetChatClientData(long chatId, String clientData) {
            this.chatId = chatId;
            this.clientData = clientData;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -827119811;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -827119811;
        }
    }

    /**
     * Changes the draft message in a chat.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetChatDraftMessage extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New draft message; may be null.
         */
        public @Nullable DraftMessage draftMessage;

        /**
         * Default constructor.
         */
        public SetChatDraftMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param draftMessage New draft message; may be null.
         */
        public SetChatDraftMessage(long chatId, DraftMessage draftMessage) {
            this.chatId = chatId;
            this.draftMessage = draftMessage;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -588175579;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -588175579;
        }
    }

    /**
     * Changes the status of a chat member, needs appropriate privileges. This function is currently not suitable for adding new members to the chat; instead, use addChatMember. The chat member status will not be changed until it has been synchronized with the server.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetChatMemberStatus extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * User identifier.
         */
        public int userId;
        /**
         * The new status of the member in the chat.
         */
        public ChatMemberStatus status;

        /**
         * Default constructor.
         */
        public SetChatMemberStatus() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param userId User identifier.
         * @param status The new status of the member in the chat.
         */
        public SetChatMemberStatus(long chatId, int userId, ChatMemberStatus status) {
            this.chatId = chatId;
            this.userId = userId;
            this.status = status;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1754439241;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1754439241;
        }
    }

    /**
     * Changes the photo of a chat. Supported only for basic groups, supergroups and channels. Requires administrator rights in basic groups and the appropriate administrator rights in supergroups and channels. The photo will not be changed before request to the server has been completed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetChatPhoto extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New chat photo. You can use a zero InputFileId to delete the chat photo. Files that are accessible only by HTTP URL are not acceptable.
         */
        public InputFile photo;

        /**
         * Default constructor.
         */
        public SetChatPhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param photo New chat photo. You can use a zero InputFileId to delete the chat photo. Files that are accessible only by HTTP URL are not acceptable.
         */
        public SetChatPhoto(long chatId, InputFile photo) {
            this.chatId = chatId;
            this.photo = photo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 132244217;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 132244217;
        }
    }

    /**
     * Changes the chat title. Supported only for basic groups, supergroups and channels. Requires administrator rights in basic groups and the appropriate administrator rights in supergroups and channels. The title will not be changed until the request to the server has been completed.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetChatTitle extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New title of the chat; 1-255 characters.
         */
        public String title;

        /**
         * Default constructor.
         */
        public SetChatTitle() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param title New title of the chat; 1-255 characters.
         */
        public SetChatTitle(long chatId, String title) {
            this.chatId = chatId;
            this.title = title;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 164282047;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 164282047;
        }
    }

    /**
     * Changes the database encryption key. Usually the encryption key is never changed and is stored in some OS keychain.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetDatabaseEncryptionKey extends Function {
        /**
         * New encryption key.
         */
        public byte[] newEncryptionKey;

        /**
         * Default constructor.
         */
        public SetDatabaseEncryptionKey() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param newEncryptionKey New encryption key.
         */
        public SetDatabaseEncryptionKey(byte[] newEncryptionKey) {
            this.newEncryptionKey = newEncryptionKey;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1204599371;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1204599371;
        }
    }

    /**
     * The next part of a file was generated.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetFileGenerationProgress extends Function {
        /**
         * The identifier of the generation process.
         */
        public long generationId;
        /**
         * Expected size of the generated file, in bytes; 0 if unknown.
         */
        public int expectedSize;
        /**
         * The number of bytes already generated.
         */
        public int localPrefixSize;

        /**
         * Default constructor.
         */
        public SetFileGenerationProgress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param generationId The identifier of the generation process.
         * @param expectedSize Expected size of the generated file, in bytes; 0 if unknown.
         * @param localPrefixSize The number of bytes already generated.
         */
        public SetFileGenerationProgress(long generationId, int expectedSize, int localPrefixSize) {
            this.generationId = generationId;
            this.expectedSize = expectedSize;
            this.localPrefixSize = localPrefixSize;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -540459953;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -540459953;
        }
    }

    /**
     * Updates the game score of the specified user in the game; for bots only.
     *
     * <p> Returns {@link Message Message} </p>
     */
    public static class SetGameScore extends Function {
        /**
         * The chat to which the message with the game.
         */
        public long chatId;
        /**
         * Identifier of the message.
         */
        public long messageId;
        /**
         * True, if the message should be edited.
         */
        public boolean editMessage;
        /**
         * User identifier.
         */
        public int userId;
        /**
         * The new score.
         */
        public int score;
        /**
         * Pass true to update the score even if it decreases. If the score is 0, the user will be deleted from the high score table.
         */
        public boolean force;

        /**
         * Default constructor.
         */
        public SetGameScore() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId The chat to which the message with the game.
         * @param messageId Identifier of the message.
         * @param editMessage True, if the message should be edited.
         * @param userId User identifier.
         * @param score The new score.
         * @param force Pass true to update the score even if it decreases. If the score is 0, the user will be deleted from the high score table.
         */
        public SetGameScore(long chatId, long messageId, boolean editMessage, int userId, int score, boolean force) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.editMessage = editMessage;
            this.userId = userId;
            this.score = score;
            this.force = force;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1768307069;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1768307069;
        }
    }

    /**
     * Updates the game score of the specified user in a game; for bots only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetInlineGameScore extends Function {
        /**
         * Inline message identifier.
         */
        public String inlineMessageId;
        /**
         * True, if the message should be edited.
         */
        public boolean editMessage;
        /**
         * User identifier.
         */
        public int userId;
        /**
         * The new score.
         */
        public int score;
        /**
         * Pass true to update the score even if it decreases. If the score is 0, the user will be deleted from the high score table.
         */
        public boolean force;

        /**
         * Default constructor.
         */
        public SetInlineGameScore() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param inlineMessageId Inline message identifier.
         * @param editMessage True, if the message should be edited.
         * @param userId User identifier.
         * @param score The new score.
         * @param force Pass true to update the score even if it decreases. If the score is 0, the user will be deleted from the high score table.
         */
        public SetInlineGameScore(String inlineMessageId, boolean editMessage, int userId, int score, boolean force) {
            this.inlineMessageId = inlineMessageId;
            this.editMessage = editMessage;
            this.userId = userId;
            this.score = score;
            this.force = force;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 758435487;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 758435487;
        }
    }

    /**
     * Changes the first and last name of the current user. If something changes, updateUser will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetName extends Function {
        /**
         * The new value of the first name for the user; 1-255 characters.
         */
        public String firstName;
        /**
         * The new value of the optional last name for the user; 0-255 characters.
         */
        public String lastName;

        /**
         * Default constructor.
         */
        public SetName() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param firstName The new value of the first name for the user; 1-255 characters.
         * @param lastName The new value of the optional last name for the user; 0-255 characters.
         */
        public SetName(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1711693584;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1711693584;
        }
    }

    /**
     * Sets the current network type. Can be called before authorization. Calling this method forces all network connections to reopen, mitigating the delay in switching between different networks, so it should be called whenever the network is changed, even if the network type remains the same. Network type is used to check whether the library can use the network at all and also for collecting detailed network data usage statistics.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetNetworkType extends Function {
        /**
         * The new network type. By default, networkTypeOther.
         */
        public NetworkType type;

        /**
         * Default constructor.
         */
        public SetNetworkType() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param type The new network type. By default, networkTypeOther.
         */
        public SetNetworkType(NetworkType type) {
            this.type = type;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -701635234;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -701635234;
        }
    }

    /**
     * Changes notification settings for a given scope.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetNotificationSettings extends Function {
        /**
         * Scope for which to change the notification settings.
         */
        public NotificationSettingsScope scope;
        /**
         * The new notification settings for the given scope.
         */
        public NotificationSettings notificationSettings;

        /**
         * Default constructor.
         */
        public SetNotificationSettings() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param scope Scope for which to change the notification settings.
         * @param notificationSettings The new notification settings for the given scope.
         */
        public SetNotificationSettings(NotificationSettingsScope scope, NotificationSettings notificationSettings) {
            this.scope = scope;
            this.notificationSettings = notificationSettings;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -134430483;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -134430483;
        }
    }

    /**
     * Sets the value of an option. (Check the list of available options on https://core.telegram.org/tdlib/options.) Only writable options can be set. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetOption extends Function {
        /**
         * The name of the option.
         */
        public String name;
        /**
         * The new value of the option.
         */
        public OptionValue value;

        /**
         * Default constructor.
         */
        public SetOption() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param name The name of the option.
         * @param value The new value of the option.
         */
        public SetOption(String name, OptionValue value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2114670322;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2114670322;
        }
    }

    /**
     * Changes the password for the user. If a new recovery email address is specified, then the error EMAILUNCONFIRMED is returned and the password change will not be applied until the new recovery email address has been confirmed. The application should periodically call getPasswordState to check whether the new email address has been confirmed.
     *
     * <p> Returns {@link PasswordState PasswordState} </p>
     */
    public static class SetPassword extends Function {
        /**
         * Previous password of the user.
         */
        public String oldPassword;
        /**
         * New password of the user; may be empty to remove the password.
         */
        public String newPassword;
        /**
         * New password hint; may be empty.
         */
        public String newHint;
        /**
         * Pass true if the recovery email address should be changed.
         */
        public boolean setRecoveryEmailAddress;
        /**
         * New recovery email address; may be empty.
         */
        public String newRecoveryEmailAddress;

        /**
         * Default constructor.
         */
        public SetPassword() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param oldPassword Previous password of the user.
         * @param newPassword New password of the user; may be empty to remove the password.
         * @param newHint New password hint; may be empty.
         * @param setRecoveryEmailAddress Pass true if the recovery email address should be changed.
         * @param newRecoveryEmailAddress New recovery email address; may be empty.
         */
        public SetPassword(String oldPassword, String newPassword, String newHint, boolean setRecoveryEmailAddress, String newRecoveryEmailAddress) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
            this.newHint = newHint;
            this.setRecoveryEmailAddress = setRecoveryEmailAddress;
            this.newRecoveryEmailAddress = newRecoveryEmailAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1193589027;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1193589027;
        }
    }

    /**
     * Changes the order of pinned chats.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetPinnedChats extends Function {
        /**
         * The new list of pinned chats.
         */
        public long[] chatIds;

        /**
         * Default constructor.
         */
        public SetPinnedChats() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatIds The new list of pinned chats.
         */
        public SetPinnedChats(long[] chatIds) {
            this.chatIds = chatIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1369665719;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1369665719;
        }
    }

    /**
     * Uploads a new profile photo for the current user. If something changes, updateUser will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetProfilePhoto extends Function {
        /**
         * Profile photo to set. inputFileId and inputFileRemote may still be unsupported.
         */
        public InputFile photo;

        /**
         * Default constructor.
         */
        public SetProfilePhoto() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param photo Profile photo to set. inputFileId and inputFileRemote may still be unsupported.
         */
        public SetProfilePhoto(InputFile photo) {
            this.photo = photo;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1594734550;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1594734550;
        }
    }

    /**
     * Sets the proxy server for network requests. Can be called before authorization.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetProxy extends Function {
        /**
         * Proxy server to use. Specify null to remove the proxy server.
         */
        public Proxy proxy;

        /**
         * Default constructor.
         */
        public SetProxy() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param proxy Proxy server to use. Specify null to remove the proxy server.
         */
        public SetProxy(Proxy proxy) {
            this.proxy = proxy;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -656782179;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -656782179;
        }
    }

    /**
     * Changes the recovery email address of the user. If a new recovery email address is specified, then the error EMAILUNCONFIRMED is returned and the email address will not be changed until the new email has been confirmed. The application should periodically call getPasswordState to check whether the email address has been confirmed. If newRecoveryEmailAddress is the same as the email address that is currently set up, this call succeeds immediately and aborts all other requests waiting for an email confirmation.
     *
     * <p> Returns {@link PasswordState PasswordState} </p>
     */
    public static class SetRecoveryEmailAddress extends Function {
        /**
         * Password of the current user.
         */
        public String password;
        /**
         * New recovery email address.
         */
        public String newRecoveryEmailAddress;

        /**
         * Default constructor.
         */
        public SetRecoveryEmailAddress() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param password Password of the current user.
         * @param newRecoveryEmailAddress New recovery email address.
         */
        public SetRecoveryEmailAddress(String password, String newRecoveryEmailAddress) {
            this.password = password;
            this.newRecoveryEmailAddress = newRecoveryEmailAddress;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1981836385;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1981836385;
        }
    }

    /**
     * Changes the position of a sticker in the set to which it belongs; for bots only. The sticker set must have been created by the bot.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetStickerPositionInSet extends Function {
        /**
         * Sticker.
         */
        public InputFile sticker;
        /**
         * New position of the sticker in the set, zero-based.
         */
        public int position;

        /**
         * Default constructor.
         */
        public SetStickerPositionInSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sticker Sticker.
         * @param position New position of the sticker in the set, zero-based.
         */
        public SetStickerPositionInSet(InputFile sticker, int position) {
            this.sticker = sticker;
            this.position = position;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 2075281185;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 2075281185;
        }
    }

    /**
     * Changes information about a supergroup or channel; requires appropriate administrator rights.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetSupergroupDescription extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;
        /**
         * New supergroup or channel description; 0-255 characters.
         */
        public String description;

        /**
         * Default constructor.
         */
        public SetSupergroupDescription() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         * @param description New supergroup or channel description; 0-255 characters.
         */
        public SetSupergroupDescription(int supergroupId, String description) {
            this.supergroupId = supergroupId;
            this.description = description;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 227623488;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 227623488;
        }
    }

    /**
     * Changes the sticker set of a supergroup; requires appropriate rights in the supergroup.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetSupergroupStickerSet extends Function {
        /**
         * Identifier of the supergroup.
         */
        public int supergroupId;
        /**
         * New value of the supergroup sticker set identifier. Use 0 to remove the supergroup sticker set.
         */
        public long stickerSetId;

        /**
         * Default constructor.
         */
        public SetSupergroupStickerSet() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup.
         * @param stickerSetId New value of the supergroup sticker set identifier. Use 0 to remove the supergroup sticker set.
         */
        public SetSupergroupStickerSet(int supergroupId, long stickerSetId) {
            this.supergroupId = supergroupId;
            this.stickerSetId = stickerSetId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -295782298;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -295782298;
        }
    }

    /**
     * Changes the username of a supergroup or channel, requires creator privileges in the supergroup or channel.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetSupergroupUsername extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;
        /**
         * New value of the username. Use an empty string to remove the username.
         */
        public String username;

        /**
         * Default constructor.
         */
        public SetSupergroupUsername() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         * @param username New value of the username. Use an empty string to remove the username.
         */
        public SetSupergroupUsername(int supergroupId, String username) {
            this.supergroupId = supergroupId;
            this.username = username;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1428333122;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1428333122;
        }
    }

    /**
     * Sets the parameters for TDLib initialization. Works only when the current authorization state is authorizationStateWaitTdlibParameters.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetTdlibParameters extends Function {
        /**
         * Parameters.
         */
        public TdlibParameters parameters;

        /**
         * Default constructor.
         */
        public SetTdlibParameters() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param parameters Parameters.
         */
        public SetTdlibParameters(TdlibParameters parameters) {
            this.parameters = parameters;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1912557997;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1912557997;
        }
    }

    /**
     * Changes user privacy settings.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetUserPrivacySettingRules extends Function {
        /**
         * The privacy setting.
         */
        public UserPrivacySetting setting;
        /**
         * The new privacy rules.
         */
        public UserPrivacySettingRules rules;

        /**
         * Default constructor.
         */
        public SetUserPrivacySettingRules() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param setting The privacy setting.
         * @param rules The new privacy rules.
         */
        public SetUserPrivacySettingRules(UserPrivacySetting setting, UserPrivacySettingRules rules) {
            this.setting = setting;
            this.rules = rules;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -473812741;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -473812741;
        }
    }

    /**
     * Changes the username of the current user. If something changes, updateUser will be sent.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class SetUsername extends Function {
        /**
         * The new value of the username. Use an empty string to remove the username.
         */
        public String username;

        /**
         * Default constructor.
         */
        public SetUsername() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param username The new value of the username. Use an empty string to remove the username.
         */
        public SetUsername(String username) {
            this.username = username;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 439901214;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 439901214;
        }
    }

    /**
     * Terminates all other sessions of the current user.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class TerminateAllOtherSessions extends Function {

        /**
         * Default constructor.
         */
        public TerminateAllOtherSessions() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1874485523;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1874485523;
        }
    }

    /**
     * Terminates a session of the current user.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class TerminateSession extends Function {
        /**
         * Session identifier.
         */
        public long sessionId;

        /**
         * Default constructor.
         */
        public TerminateSession() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param sessionId Session identifier.
         */
        public TerminateSession(long sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -407385812;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -407385812;
        }
    }

    /**
     * Returns the received bytes; for testing only.
     *
     * <p> Returns {@link TestBytes TestBytes} </p>
     */
    public static class TestCallBytes extends Function {
        /**
         * Bytes to return.
         */
        public byte[] x;

        /**
         * Default constructor.
         */
        public TestCallBytes() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Bytes to return.
         */
        public TestCallBytes(byte[] x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -736011607;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -736011607;
        }
    }

    /**
     * Does nothing; for testing only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class TestCallEmpty extends Function {

        /**
         * Default constructor.
         */
        public TestCallEmpty() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -627291626;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -627291626;
        }
    }

    /**
     * Returns the received string; for testing only.
     *
     * <p> Returns {@link TestString TestString} </p>
     */
    public static class TestCallString extends Function {
        /**
         * String to return.
         */
        public String x;

        /**
         * Default constructor.
         */
        public TestCallString() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x String to return.
         */
        public TestCallString(String x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1732818385;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1732818385;
        }
    }

    /**
     * Returns the received vector of numbers; for testing only.
     *
     * <p> Returns {@link TestVectorInt TestVectorInt} </p>
     */
    public static class TestCallVectorInt extends Function {
        /**
         * Vector of numbers to return.
         */
        public int[] x;

        /**
         * Default constructor.
         */
        public TestCallVectorInt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Vector of numbers to return.
         */
        public TestCallVectorInt(int[] x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -2137277793;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -2137277793;
        }
    }

    /**
     * Returns the received vector of objects containing a number; for testing only.
     *
     * <p> Returns {@link TestVectorIntObject TestVectorIntObject} </p>
     */
    public static class TestCallVectorIntObject extends Function {
        /**
         * Vector of objects to return.
         */
        public TestInt[] x;

        /**
         * Default constructor.
         */
        public TestCallVectorIntObject() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Vector of objects to return.
         */
        public TestCallVectorIntObject(TestInt[] x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1825428218;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1825428218;
        }
    }

    /**
     * For testing only request. Returns the received vector of strings; for testing only.
     *
     * <p> Returns {@link TestVectorString TestVectorString} </p>
     */
    public static class TestCallVectorString extends Function {
        /**
         * Vector of strings to return.
         */
        public String[] x;

        /**
         * Default constructor.
         */
        public TestCallVectorString() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Vector of strings to return.
         */
        public TestCallVectorString(String[] x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -408600900;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -408600900;
        }
    }

    /**
     * Returns the received vector of objects containing a string; for testing only.
     *
     * <p> Returns {@link TestVectorStringObject TestVectorStringObject} </p>
     */
    public static class TestCallVectorStringObject extends Function {
        /**
         * Vector of objects to return.
         */
        public TestString[] x;

        /**
         * Default constructor.
         */
        public TestCallVectorStringObject() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Vector of objects to return.
         */
        public TestCallVectorStringObject(TestString[] x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1527666429;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1527666429;
        }
    }

    /**
     * Forces an updates.getDifference call to the Telegram servers; for testing only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class TestGetDifference extends Function {

        /**
         * Default constructor.
         */
        public TestGetDifference() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1747084069;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1747084069;
        }
    }

    /**
     * Sends a simple network request to the Telegram servers; for testing only.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class TestNetwork extends Function {

        /**
         * Default constructor.
         */
        public TestNetwork() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1343998901;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1343998901;
        }
    }

    /**
     * Returns the squared received number; for testing only.
     *
     * <p> Returns {@link TestInt TestInt} </p>
     */
    public static class TestSquareInt extends Function {
        /**
         * Number to square.
         */
        public int x;

        /**
         * Default constructor.
         */
        public TestSquareInt() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param x Number to square.
         */
        public TestSquareInt(int x) {
            this.x = x;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -60135024;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -60135024;
        }
    }

    /**
     * Does nothing and ensures that the Error object is used; for testing only.
     *
     * <p> Returns {@link Error Error} </p>
     */
    public static class TestUseError extends Function {

        /**
         * Default constructor.
         */
        public TestUseError() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 528842186;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 528842186;
        }
    }

    /**
     * Does nothing and ensures that the Update object is used; for testing only.
     *
     * <p> Returns {@link Update Update} </p>
     */
    public static class TestUseUpdate extends Function {

        /**
         * Default constructor.
         */
        public TestUseUpdate() {
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 717094686;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 717094686;
        }
    }

    /**
     * Toggles the &quot;All members are admins&quot; setting in basic groups; requires creator privileges in the group.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ToggleBasicGroupAdministrators extends Function {
        /**
         * Identifier of the basic group.
         */
        public int basicGroupId;
        /**
         * New value of everyoneIsAdministrator.
         */
        public boolean everyoneIsAdministrator;

        /**
         * Default constructor.
         */
        public ToggleBasicGroupAdministrators() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param basicGroupId Identifier of the basic group.
         * @param everyoneIsAdministrator New value of everyoneIsAdministrator.
         */
        public ToggleBasicGroupAdministrators(int basicGroupId, boolean everyoneIsAdministrator) {
            this.basicGroupId = basicGroupId;
            this.everyoneIsAdministrator = everyoneIsAdministrator;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -591395611;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -591395611;
        }
    }

    /**
     * Changes the pinned state of a chat. You can pin up to GetOption(&quot;pinnedChatCountMax&quot;) non-secret chats and the same number of secret chats.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ToggleChatIsPinned extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * New value of isPinned.
         */
        public boolean isPinned;

        /**
         * Default constructor.
         */
        public ToggleChatIsPinned() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param isPinned New value of isPinned.
         */
        public ToggleChatIsPinned(long chatId, boolean isPinned) {
            this.chatId = chatId;
            this.isPinned = isPinned;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1166802621;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1166802621;
        }
    }

    /**
     * Toggles whether all members of a supergroup can add new members; requires appropriate administrator rights in the supergroup.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ToggleSupergroupInvites extends Function {
        /**
         * Identifier of the supergroup.
         */
        public int supergroupId;
        /**
         * New value of anyoneCanInvite.
         */
        public boolean anyoneCanInvite;

        /**
         * Default constructor.
         */
        public ToggleSupergroupInvites() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup.
         * @param anyoneCanInvite New value of anyoneCanInvite.
         */
        public ToggleSupergroupInvites(int supergroupId, boolean anyoneCanInvite) {
            this.supergroupId = supergroupId;
            this.anyoneCanInvite = anyoneCanInvite;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -797384141;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -797384141;
        }
    }

    /**
     * Toggles whether the message history of a supergroup is available to new members; requires appropriate administrator rights in the supergroup.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ToggleSupergroupIsAllHistoryAvailable extends Function {
        /**
         * The identifier of the supergroup.
         */
        public int supergroupId;
        /**
         * The new value of isAllHistoryAvailable.
         */
        public boolean isAllHistoryAvailable;

        /**
         * Default constructor.
         */
        public ToggleSupergroupIsAllHistoryAvailable() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId The identifier of the supergroup.
         * @param isAllHistoryAvailable The new value of isAllHistoryAvailable.
         */
        public ToggleSupergroupIsAllHistoryAvailable(int supergroupId, boolean isAllHistoryAvailable) {
            this.supergroupId = supergroupId;
            this.isAllHistoryAvailable = isAllHistoryAvailable;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1701526555;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1701526555;
        }
    }

    /**
     * Toggles sender signatures messages sent in a channel; requires appropriate administrator rights in the channel.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ToggleSupergroupSignMessages extends Function {
        /**
         * Identifier of the channel.
         */
        public int supergroupId;
        /**
         * New value of signMessages.
         */
        public boolean signMessages;

        /**
         * Default constructor.
         */
        public ToggleSupergroupSignMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the channel.
         * @param signMessages New value of signMessages.
         */
        public ToggleSupergroupSignMessages(int supergroupId, boolean signMessages) {
            this.supergroupId = supergroupId;
            this.signMessages = signMessages;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -558196581;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -558196581;
        }
    }

    /**
     * Removes a user from the blacklist.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class UnblockUser extends Function {
        /**
         * User identifier.
         */
        public int userId;

        /**
         * Default constructor.
         */
        public UnblockUser() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId User identifier.
         */
        public UnblockUser(int userId) {
            this.userId = userId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -307286367;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -307286367;
        }
    }

    /**
     * Removes the pinned message from a supergroup or channel; requires appropriate administrator rights in the supergroup or channel.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class UnpinSupergroupMessage extends Function {
        /**
         * Identifier of the supergroup or channel.
         */
        public int supergroupId;

        /**
         * Default constructor.
         */
        public UnpinSupergroupMessage() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param supergroupId Identifier of the supergroup or channel.
         */
        public UnpinSupergroupMessage(int supergroupId) {
            this.supergroupId = supergroupId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1987029530;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1987029530;
        }
    }

    /**
     * Creates a new supergroup from an existing basic group and sends a corresponding messageChatUpgradeTo and messageChatUpgradeFrom. Deactivates the original basic group.
     *
     * <p> Returns {@link Chat Chat} </p>
     */
    public static class UpgradeBasicGroupChatToSupergroupChat extends Function {
        /**
         * Identifier of the chat to upgrade.
         */
        public long chatId;

        /**
         * Default constructor.
         */
        public UpgradeBasicGroupChatToSupergroupChat() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Identifier of the chat to upgrade.
         */
        public UpgradeBasicGroupChatToSupergroupChat(long chatId) {
            this.chatId = chatId;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 300488122;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 300488122;
        }
    }

    /**
     * Asynchronously uploads a file to the cloud without sending it in a message. updateFile will be used to notify about upload progress and successful completion of the upload. The file will not have a persistent remote identifier until it will be sent in a message.
     *
     * <p> Returns {@link File File} </p>
     */
    public static class UploadFile extends Function {
        /**
         * File to upload.
         */
        public InputFile file;
        /**
         * File type.
         */
        public FileType fileType;
        /**
         * Priority of the upload (1-32). The higher the priority, the earlier the file will be uploaded. If the priorities of two files are equal, then the first one for which uploadFile was called will be uploaded first.
         */
        public int priority;

        /**
         * Default constructor.
         */
        public UploadFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param file File to upload.
         * @param fileType File type.
         * @param priority Priority of the upload (1-32). The higher the priority, the earlier the file will be uploaded. If the priorities of two files are equal, then the first one for which uploadFile was called will be uploaded first.
         */
        public UploadFile(InputFile file, FileType fileType, int priority) {
            this.file = file;
            this.fileType = fileType;
            this.priority = priority;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -745597786;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -745597786;
        }
    }

    /**
     * Uploads a PNG image with a sticker; for bots only; returns the uploaded file.
     *
     * <p> Returns {@link File File} </p>
     */
    public static class UploadStickerFile extends Function {
        /**
         * Sticker file owner.
         */
        public int userId;
        /**
         * PNG image with the sticker; must be up to 512 kB in size and fit in 512x512 square.
         */
        public InputFile pngSticker;

        /**
         * Default constructor.
         */
        public UploadStickerFile() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param userId Sticker file owner.
         * @param pngSticker PNG image with the sticker; must be up to 512 kB in size and fit in 512x512 square.
         */
        public UploadStickerFile(int userId, InputFile pngSticker) {
            this.userId = userId;
            this.pngSticker = pngSticker;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 1134087551;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 1134087551;
        }
    }

    /**
     * Validates the order information provided by a user and returns the available shipping options for a flexible invoice.
     *
     * <p> Returns {@link ValidatedOrderInfo ValidatedOrderInfo} </p>
     */
    public static class ValidateOrderInfo extends Function {
        /**
         * Chat identifier of the Invoice message.
         */
        public long chatId;
        /**
         * Message identifier.
         */
        public long messageId;
        /**
         * The order information, provided by the user.
         */
        public OrderInfo orderInfo;
        /**
         * True, if the order information can be saved.
         */
        public boolean allowSave;

        /**
         * Default constructor.
         */
        public ValidateOrderInfo() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier of the Invoice message.
         * @param messageId Message identifier.
         * @param orderInfo The order information, provided by the user.
         * @param allowSave True, if the order information can be saved.
         */
        public ValidateOrderInfo(long chatId, long messageId, OrderInfo orderInfo, boolean allowSave) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.orderInfo = orderInfo;
            this.allowSave = allowSave;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = 9480644;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return 9480644;
        }
    }

    /**
     * This method should be called if messages are being viewed by the user. Many useful activities depend on whether the messages are currently being viewed or not (e.g., marking messages as read, incrementing a view counter, updating a view counter, removing deleted messages in supergroups and channels).
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ViewMessages extends Function {
        /**
         * Chat identifier.
         */
        public long chatId;
        /**
         * The identifiers of the messages being viewed.
         */
        public long[] messageIds;
        /**
         * True, if messages in closed chats should be marked as read.
         */
        public boolean forceRead;

        /**
         * Default constructor.
         */
        public ViewMessages() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param chatId Chat identifier.
         * @param messageIds The identifiers of the messages being viewed.
         * @param forceRead True, if messages in closed chats should be marked as read.
         */
        public ViewMessages(long chatId, long[] messageIds, boolean forceRead) {
            this.chatId = chatId;
            this.messageIds = messageIds;
            this.forceRead = forceRead;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -1925784915;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -1925784915;
        }
    }

    /**
     * Informs the server that some trending sticker sets have been viewed by the user.
     *
     * <p> Returns {@link Ok Ok} </p>
     */
    public static class ViewTrendingStickerSets extends Function {
        /**
         * Identifiers of viewed trending sticker sets.
         */
        public long[] stickerSetIds;

        /**
         * Default constructor.
         */
        public ViewTrendingStickerSets() {
        }

        /**
         * Constructor for initialization of all fields.
         *
         * @param stickerSetIds Identifiers of viewed trending sticker sets.
         */
        public ViewTrendingStickerSets(long[] stickerSetIds) {
            this.stickerSetIds = stickerSetIds;
        }

        /**
         * Identifier uniquely determining type of the object.
         */
        public static final int CONSTRUCTOR = -952416520;

        /**
         * @return this.CONSTRUCTOR
         */
        @Override
        public int getConstructor() {
            return -952416520;
        }
    }

}
