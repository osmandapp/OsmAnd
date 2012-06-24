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
turn('right_keep', ['serrez.ogg','a_droite.ogg']).
turn('left_keep', ['serrez.ogg','a_gauche.ogg']).

prepare_turn(Turn, Dist) == ['dans.ogg', delay_450, D, 'preparez_vous_a.ogg',  delay_450, M] :-
distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['dans.ogg', delay_250, D, delay_250, M] :-
distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['dans.ogg', delay_300, D, 'preparez_vous_a.ogg',  delay_300,'faire_demi_tour.ogg'] :-
distance(Dist) == D.

prepare_roundabout(Dist) == ['dans.ogg', delay_300, D,'preparez_vous_a.ogg','entrez_dans_rondpoint.ogg'] :-
distance(Dist) == D.

make_ut(Dist) == ['dans.ogg', delay_300, D, 'preparez_vous_a.ogg',  delay_300,'faites_demi_tour.ogg'] :-
distance(Dist) == D.
make_ut == ['faites_demi_tour.ogg'].

roundabout(Dist, _Angle, Exit) == ['dans.ogg', delay_300, D, delay_300, 'entrez_dans_rondpoint.ogg', delay_250, 'et.ogg','prenez_la.ogg',
delay_250, E, 'sortie.ogg'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['prenez_la.ogg', delay_250, E, 'sortie.ogg'] :- nth(Exit, E).

and_arrive_destination == ['arrivez_a_destination.ogg']. % Miss and?
then == ['puis.ogg', delay_350].
reached_destination == ['vous_etes_arrives.ogg'].
bear_right == ['serrez.ogg','a_droite.ogg'].
bear_left == ['serrez.ogg','a_gauche.ogg'].
route_recalc(_Dist) == []. % ['recalcul_itineraire.ogg']. %nothing to said possibly beep?
route_new_calc(Dist) == ['le_trajet_fait.ogg', delay_150, D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['continuez_pendant.ogg', delay_250, D]:- distance(Dist) == D.
go_ahead == ['continuez_tout_droit.ogg'].

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
dist(D, ['10-fr.ogg']) :- D < 20, !.
dist(D, ['20-fr.ogg']) :- D < 30, !.
dist(D, ['30-fr.ogg']) :- D < 40, !.
dist(D, ['40-fr.ogg']) :- D < 50, !.
dist(D, ['50-fr.ogg']) :- D < 60, !.
dist(D, ['60-fr.ogg']) :- D < 70, !.
dist(D, ['70-fr.ogg']) :- D < 80, !.
dist(D, ['80-fr.ogg']) :- D < 90, !.
dist(D, ['90-fr.ogg']) :- D < 100, !.
dist(D, ['100-fr.ogg']) :- D < 150, !.
dist(D, ['100-fr.ogg', '50-fr.ogg']) :- D < 200, !.
dist(D, ['200-fr.ogg']) :- D < 250, !.
dist(D, ['200-fr.ogg', '50-fr.ogg']) :- D < 300, !.
dist(D, ['300-fr.ogg']) :- D < 350, !.
dist(D, ['300-fr.ogg', '50-fr.ogg']) :- D < 400, !.
dist(D, ['400-fr.ogg']) :- D < 450, !.
dist(D, ['400-fr.ogg', '50-fr.ogg']) :- D < 500, !.
dist(D, ['500-fr.ogg']) :- D < 550, !.
dist(D, ['500-fr.ogg', '50-fr.ogg']) :- D < 600, !.
dist(D, ['600-fr.ogg']) :- D < 650, !.
dist(D, ['600-fr.ogg', '50-fr.ogg']) :- D < 700, !.
dist(D, ['700-fr.ogg']) :- D < 750, !.
dist(D, ['700-fr.ogg', '50-fr.ogg']) :- D < 800, !.
dist(D, ['800-fr.ogg']) :- D < 850, !.
dist(D, ['800-fr.ogg', '50-fr.ogg']) :- D < 900, !.
dist(D, ['900-fr.ogg']) :- D < 950, !.
dist(D, ['900-fr.ogg', '50-fr.ogg']) :- !.


distance(Dist) == ['plus_de.ogg', '1-fr.ogg', 'kilometre.ogg'] :- Dist < 1500.
distance(Dist) == ['plus_de.ogg', '2-fr.ogg', 'kilometre.ogg'] :- Dist < 3000.
distance(Dist) == ['plus_de.ogg', '3-fr.ogg', 'kilometre.ogg'] :- Dist < 4000.
distance(Dist) == ['plus_de.ogg', '4-fr.ogg', 'kilometre.ogg'] :- Dist < 5000.
distance(Dist) == ['plus_de.ogg', '5-fr.ogg', 'kilometre.ogg'] :- Dist < 6000.
distance(Dist) == ['plus_de.ogg', '6-fr.ogg', 'kilometre.ogg'] :- Dist < 7000.
distance(Dist) == ['plus_de.ogg', '7-fr.ogg', 'kilometre.ogg'] :- Dist < 8000.
distance(Dist) == ['plus_de.ogg', '8-fr.ogg', 'kilometre.ogg'] :- Dist < 9000.
distance(Dist) == ['plus_de.ogg', '9-fr.ogg', 'kilometre.ogg'] :- Dist < 10000.
distance(Dist) == ['plus_de.ogg', X, 'kilometre.ogg'] :- D is Dist/1000, dist(D, X).



%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):-
flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).

