:- op('==', xfy, 500).
version(0).

% before each announcement (beep)
preamble - [].


%% TURNS
turn('left', ['tournez.ogg', 'a_gauche.ogg']).
turn('left_sh', ['tournez_immediat.ogg', 'a_gauche.ogg']).
turn('left_sl', ['tournez_lentement.ogg', 'a_gauche.ogg']).
turn('right', ['tournez.ogg', 'a_droite.ogg']).
turn('right_sh', ['tournez_immediat.ogg', 'a_droite.ogg']).
turn('right_sl', ['tournez_lentement.ogg', 'a_droite.ogg']).

prepare_turn(Turn, Dist) == ['dans.ogg', delay_450, D, 'preparez_vous_a.ogg',  delay_450, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['dans.ogg', delay_250, D, delay_250, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['dans.ogg', delay_300, D, 'preparez_vous_a.ogg',  delay_300,'faire_demi_tour.ogg'] :- distance(Dist) == D.
make_ut(Dist) == ['dans.ogg', delay_300, D, 'preparez_vous_a.ogg',  delay_300,'faites_demi_tour.ogg'] :- distance(Dist) == D.
make_ut == ['faites_demi_tour.ogg'].
make_ut_wp == ['faites_demi_tour.ogg'].

prepare_roundabout(Dist) == ['dans.ogg', delay_300, D,'preparez_vous_a.ogg','entrez_dans_rondpoint.ogg'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['dans.ogg', delay_300, D, delay_300, 'entrez_dans_rondpoint.ogg', delay_250, 'et.ogg','prenez_la.ogg',
	delay_250, E, 'sortie.ogg'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['prenez_la.ogg', delay_250, E, 'sortie.ogg'] :- nth(Exit, E).

go_ahead == ['continuez_tout_droit.ogg'].
go_ahead(Dist) == ['continuez_pendant.ogg', delay_250, D]:- distance(Dist) == D.

and_arrive_destination == ['arrivez_a_destination.ogg'].

then == ['puis.ogg', delay_350].
reached_destination == ['vous_etes_arrives.ogg'].
bear_right == ['serrez.ogg','a_droite.ogg'].
bear_left == ['serrez.ogg','a_gauche.ogg'].

route_new_calc(Dist) == ['le_trajet_fait.ogg', delay_150, D] :- distance(Dist) == D. % nothing to said possibly beep?
route_recalc(Dist) == ['recalcul_itineraire.ogg', delay_150, D]:- distance(Dist) == D.

location_lost == ['gps_signal_lost.ogg'].


%%
nth(1, '1ere.ogg').
nth(2, '2eme.ogg').
nth(3, '3eme.ogg').
nth(4, '4eme.ogg').
nth(5, '5eme.ogg').
nth(6, '6eme.ogg').
nth(7, '7eme.ogg').
nth(8, '8eme.ogg').
nth(9, '9eme.ogg').
nth(10, '10eme.ogg').
nth(11, '11eme.ogg').
nth(12, '12eme.ogg').
nth(13, '13eme.ogg').
nth(14, '14eme.ogg').
nth(15, '15eme.ogg').
nth(16, '16eme.ogg').
nth(17, '17eme.ogg').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metres.ogg',T).
dist(D, ['10-fr.ogg']) :- D < 15, !.
dist(D, ['20-fr.ogg']) :- D < 25, !.
dist(D, ['30-fr.ogg']) :- D < 35, !.
dist(D, ['40-fr.ogg']) :- D < 45, !.
dist(D, ['50-fr.ogg']) :- D < 55, !.
dist(D, ['60-fr.ogg']) :- D < 65, !.
dist(D, ['70-fr.ogg']) :- D < 75, !.
dist(D, ['80-fr.ogg']) :- D < 85, !.
dist(D, ['90-fr.ogg']) :- D < 95, !.
dist(D, ['100-fr.ogg']) :- D < 125, !.
dist(D, ['100-fr.ogg', '50-fr.ogg']) :- D < 175, !.
dist(D, ['200-fr.ogg']) :- D < 225, !.
dist(D, ['200-fr.ogg', '50-fr.ogg']) :- D < 275, !.
dist(D, ['300-fr.ogg']) :- D < 325, !.
dist(D, ['300-fr.ogg', '50-fr.ogg']) :- D < 375, !.
dist(D, ['400-fr.ogg']) :- D < 425, !.
dist(D, ['400-fr.ogg', '50-fr.ogg']) :- D < 475, !.
dist(D, ['500-fr.ogg']) :- D < 525, !.
dist(D, ['500-fr.ogg', '50-fr.ogg']) :- D < 575, !.
dist(D, ['600-fr.ogg']) :- D < 625, !.
dist(D, ['600-fr.ogg', '50-fr.ogg']) :- D < 675, !.
dist(D, ['700-fr.ogg']) :- D < 725, !.
dist(D, ['700-fr.ogg', '50-fr.ogg']) :- D < 775, !.
dist(D, ['800-fr.ogg']) :- D < 825, !.
dist(D, ['800-fr.ogg', '50-fr.ogg']) :- D < 875, !.
dist(D, ['900-fr.ogg']) :- D < 925, !.
dist(D, ['900-fr.ogg', '50-fr.ogg']) :- !.


distance(Dist) == ['plus_de.ogg', '1-fr.ogg', 'kilometre.ogg'] :- Dist < 1500.
distance(Dist) == ['plus_de.ogg', '2-fr.ogg', 'kilometre.ogg'] :- Dist < 2500.
distance(Dist) == ['plus_de.ogg', '3-fr.ogg', 'kilometre.ogg'] :- Dist < 3500.
distance(Dist) == ['plus_de.ogg', '4-fr.ogg', 'kilometre.ogg'] :- Dist < 4500.
distance(Dist) == ['plus_de.ogg', '5-fr.ogg', 'kilometre.ogg'] :- Dist < 5500.
distance(Dist) == ['plus_de.ogg', '6-fr.ogg', 'kilometre.ogg'] :- Dist < 6500.
distance(Dist) == ['plus_de.ogg', '7-fr.ogg', 'kilometre.ogg'] :- Dist < 7500.
distance(Dist) == ['plus_de.ogg', '8-fr.ogg', 'kilometre.ogg'] :- Dist < 8500.
distance(Dist) == ['plus_de.ogg', '9-fr.ogg', 'kilometre.ogg'] :- Dist < 9500.
distance(Dist) == ['plus_de.ogg', X, 'kilometre.ogg'] :- D is Dist/1000, dist(D, X).


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