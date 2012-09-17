:- op('==', xfy, 500).
version(101).
language(el).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['στρίψε αριστερά ']).
turn('left_sh', ['στρίψε κλειστά αριστερά ']).
turn('left_sl', ['στρίψε λοξά αριστερά ']).
turn('right', ['στίψε δεξιά ']).
turn('right_sh', ['στρίψε κλειστά δεξιά ']).
turn('right_sl', ['στρίψε λοξά δεξιά ']).

prepare_turn(Turn, Dist) == ['Προετοιμάσου και ', M, ' μετά από ', D] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Μετά από ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Προετοιμάσου να κάνεις αναστροφή μετά από ', D] :- distance(Dist) == D.
make_ut(Dist) == ['μετά από ', D, ' κάνε αναστροφή '] :- distance(Dist) == D.
make_ut == ['Κάνε αναστροφή '].
make_ut_wp == ['Όταν είναι δυνατόν, κάνε αναστροφή '].

prepare_roundabout(Dist) == ['Προετοιμάσου να μπείς σε κυκλικό κόμβο μετά από ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Μετά από ', D, ' μπές στον κυκλικό κόμβο, και βγές στην ', E, 'έξοδο'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['βγες στην ', E, 'έξοδο'] :- nth(Exit, E).

go_ahead == ['Προχώρα ευθεία '].
go_ahead(Dist) == ['Ακολούθησε τον δρόμο για ', D]:- distance(Dist) == D.

and_arrive_destination == ['και φτάνεις στον προορισμό σου '].

then == ['και '].
reached_destination == ['έφτασες στον προορισμό σου '].
bear_right == ['μείνε δεξιά '].
bear_left == ['μείνε αριστερά '].

route_new_calc(Dist) == ['Το ταξίδι είναι ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Επαναϋπολογισμός διαδρομής, απόσταση ', D] :- distance(Dist) == D.

location_lost == ['Το σήμα GPS χάθηκε '].


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


%%% distance measure
distance(Dist) == [ X, ' μέτρα'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' μέτρα'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['περίπου 1 χιλιόμετρο '] :- Dist < 1500.
distance(Dist) == ['περίπου ', X, ' χιλιόμετρα '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' χιλιόμετρα '] :- D is round(Dist/1000), num_atom(D, X).


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