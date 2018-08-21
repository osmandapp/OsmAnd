package net.osmand.plus.routing.data;

import net.osmand.plus.routing.VoiceRouter;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class StreetName {

    private Map<String, String> names;
    private static final Term empty = new Struct("");;

    public StreetName(Map<String, String> data) {
        this.names = data;
    }

    public StreetName() {
        names = new HashMap<>();
    }

    public Map<String, String> toMap() {
        return names;
    }

    public Term toTerm() {
        if (names == null || names.isEmpty()) {
            return empty;
        }
        if (names.size() > 3) {
            Term next = new Struct(new Term[]{getTermString(names.get(VoiceRouter.TO_REF)),
                    getTermString(names.get(VoiceRouter.TO_STREET_NAME)),
                    getTermString(names.get(VoiceRouter.TO_DEST))});
            Term current = new Struct(new Term[]{getTermString(names.get(VoiceRouter.FROM_REF)),
                    getTermString(names.get(VoiceRouter.FROM_STREET_NAME)),
                    getTermString(names.get(VoiceRouter.FROM_DEST))});
            return new Struct("voice", next, current);
        } else {
            Term rf = getTermString(names.get(VoiceRouter.TO_REF));
            if (rf == empty) {
                rf = getTermString(names.get(VoiceRouter.TO_STREET_NAME));
            }
            return rf;
        }
    }

    private Term getTermString(String s) {
        if (!Algorithms.isEmpty(s)) {
            return new Struct(s);
        }
        return empty;
    }
}
