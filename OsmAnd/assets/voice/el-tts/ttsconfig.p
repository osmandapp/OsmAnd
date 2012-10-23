:- op('==', xfy, 500).
version(101).
language(el).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['στρίψτε ', 'αριστερά ']).
turn('left_sh', ['στρίψτε κλειστά αριστερά ']).
turn('left_sl', ['στρίψτε λοξά αριστερά ']).
turn('right', ['στρίψτε δεξιά ']).
turn('right_sh', ['στρίψτε κλειστά δεξιά ']).
turn('right_sl', ['στρίψτε λοξά δεξιά ']).
turn('right_keep', ['μείνετε δεξιά']).
turn('left_keep', ['μείνετε αριστερά']).

prepare_turn(Turn, Dist) == ['Προετοιμαστείτε να ', M, ' μετά από ', D] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Μετά από ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Προετοιμαστείτε να κάνετε αναστροφή μετά από ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Μετά από ', D, ' κάντε αναστροφή '] :- distance(Dist) == D.
make_ut == ['Κάντε αναστροφή '].
make_ut_wp == ['Όταν είναι δυνατόν, κάντε αναστροφή '].

prepare_roundabout(Dist) == ['Προετοιμαστείτε να μπείτε σε κυκλικό κόμβο μετά από ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Μετά από ', D, ' μπείτε στον κυκλικό κόμβο, και βγείτε στην ', E, 'έξοδο'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['βγείτε στην ', E, 'έξοδο'] :- nth(Exit, E).

go_ahead == ['Προχωρείστε ευθεία '].
go_ahead(Dist) == ['Ακολουθήστε τον δρόμο για ', D]:- distance(Dist) == D.

and_arrive_destination == ['και φτάσατε στον προορισμό σας '].

then == ['και '].
reached_destination == ['φτάσατε στον προορισμό σας '].
and_arrive_intermediate == ['και φτάσατε στο ενδιάμεσο σημείο '].
reached_intermediate == ['φτάσατε στο ενδιάμεσο σημείο'].
bear_right == ['μείνετε δεξιά '].
bear_left == ['μείνετε αριστερά '].

route_new_calc(Dist) == ['Το ταξίδι είναι ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Επαναϋπολογισμός διαδρομής, απόσταση ', D] :- distance(Dist) == D.

location_lost == ['Το σήμα gps χάθηκε '].


%% 
nth(1, 'πρώτη ').
nth(2, 'δεύτερη ').
nth(3, 'τρίτη ').
nth(4, 'τέταρτη ').
nth(5, 'πέμπτη ').
nth(6, 'έκτη ').
nth(7, 'έβδομη ').
nth(8, 'όγδοη ').
nth(9, 'ένατη ').
nth(10, 'δέκατη ').
nth(11, 'ενδέκατη ').
nth(12, 'δωδέκατη ').
nth(13, 'δέκατη τρίτη ').
nth(14, 'δέκατη τέταρτη ').
nth(15, 'δέκατη πέμπτη ').
nth(16, 'δέκατη έκτη ').
nth(17, 'δέκατη έβδομη ').


distance(Dist) == D :- measure('km-m'), distance_km(Dist) == D.
distance(Dist) == D :- distance_mi(Dist) == D.
%%% distance measure
distance_km(Dist) == [ X, ' μέτρα'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance_km(Dist) == [ X, ' μέτρα'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance_km(Dist) == ['περίπου ένα χιλιόμετρο '] :- Dist < 1500.
distance_km(Dist) == ['περίπου ', X, ' χιλιόμετρα '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance_km(Dist) == [ X, ' χιλιόμετρα '] :- D is round(Dist/1000.0), num_atom(D, X).

%%% distance measure
distance_mi(Dist) == [ X, ' πόδια'] :- Dist < 160, D is round(2*Dist/100.0/0.3048)*50, num_atom(D, X).
distance_mi(Dist) == [ X, ' δέκατο του μιλίου'] :- Dist < 241, D is round(Dist/161.0), num_atom(D, X).
distance_mi(Dist) == [ X, ' δέκατα του μιλίου'] :- Dist < 1529, D is round(Dist/161.0), num_atom(D, X).
distance_mi(Dist) == ['περίπου ένα μίλι '] :- Dist < 2414.
distance_mi(Dist) == ['περίπου ', X, ' μίλια '] :- Dist < 16093, D is round(Dist/1609.0), num_atom(D, X).
distance_mi(Dist) == [ X, ' μίλια '] :- D is round(Dist/1609.0), num_atom(D, X).


%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).