package net.osmand.plus.settings.backend.preferences;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keeps an owner-held preference listener reference from being removed by R8.
 * PreferenceWithListener stores callbacks weakly; the owner supplies the strong reference.
 * Matched by R8 at build time, without runtime reflection.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface KeepListenerReference {
}
