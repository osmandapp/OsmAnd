:- op('==', xfy, 500).
version(0).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['dolava.ogg']).
turn('left_sh', ['ostr_dolava.ogg']).
turn('left_sl', ['m_dolava.ogg']).
turn('right', ['doprava.ogg']).
turn('right_sh', ['ostr_doprava.ogg']).
turn('right_sl', ['m_doprava.ogg']).
turn('right_keep', ['drz_vpravo.ogg']).
turn('left_keep', ['drz_vlavo.ogg']).

pturn('left', ['pdolava.ogg']).
pturn('left_sh', ['postr_dolava.ogg']).
pturn('left_sl', ['pm_dolava.ogg']).
pturn('right', ['pdoprava.ogg']).
pturn('right_sh', ['postr_doprava.ogg']).
pturn('right_sl', ['pm_doprava.ogg']).
pturn('right_keep', ['drz_vpravo.ogg']).
pturn('left_keep', ['drz_vlavo.ogg']).

prepare_turn(Turn, Dist) == ['o', D, 'budete_odbacat.ogg', M] :-
			distance(Dist) == D, pturn(Turn, M).
turn(Turn, Dist) == ['o.ogg', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['o.ogg', D, 'sa_otacat.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['o.ogg', D, 'vojde_kruh.ogg'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['o.ogg', D, 'sa_otocte.ogg'] :- 
			distance(Dist) == D.
make_ut == ['otocte_sa.ogg'].

roundabout(Dist, _Angle, Exit) == ['o.ogg', D, 'vojdi_kruh.ogg', 'a_zvolte.ogg', E, 'vyjazd.ogg'] :- 
			distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['pojdete_cez.ogg', E, 'vyjazd.ogg'] :- nth(Exit, E).

and_arrive_destination == ['a_do_ciela.ogg']. % Miss and?
then == ['potom.ogg'].
reached_destination == ['doraz_ciel.ogg'].
bear_right == ['drz_vpravo.ogg'].
bear_left == ['drz_vlavo.ogg'].
route_recalc(_Dist) == ['prepocet.ogg']. % nothing to said possibly beep?	
route_new_calc(Dist) == ['cesta_je_dlha.ogg', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['pokracujte.ogg', D]:- distance(Dist) == D.
go_ahead == ['pokracujte_rovno.ogg'].

%% 
nth(1, '1st.ogg').
nth(2, '2nd.ogg').
nth(3, '3rd.ogg').
nth(4, '4th.ogg').
nth(5, '5th.ogg').
nth(6, '6th.ogg').
nth(7, '7th.ogg').
nth(8, '8th.ogg').
nth(9, '9th.ogg').
nth(10, '10th.ogg').
nth(11, '11th.ogg').
nth(12, '12th.ogg').
nth(13, '13th.ogg').
nth(14, '14th.ogg').
nth(15, '15th.ogg').
nth(16, '16th.ogg').
nth(17, '17th.ogg').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metrov.ogg',T).
dist(D, ['10.ogg']) :-  D < 20, !.
dist(D, ['20.ogg']) :-  D < 30, !.
dist(D, ['30.ogg']) :-  D < 40, !.
dist(D, ['40.ogg']) :-  D < 50, !.
dist(D, ['50.ogg']) :-  D < 60, !.
dist(D, ['60.ogg']) :-  D < 70, !.
dist(D, ['70.ogg']) :-  D < 80, !.
dist(D, ['80.ogg']) :-  D < 90, !.
dist(D, ['90.ogg']) :-  D < 100, !.
dist(D, ['100.ogg']) :-  D < 150, !.
dist(D, ['100.ogg', '50.ogg']) :-  D < 200, !.
dist(D, ['200.ogg']) :-  D < 250, !.
dist(D, ['200.ogg', '50.ogg']) :-  D < 300, !.
dist(D, ['300.ogg']) :-  D < 350, !.
dist(D, ['300.ogg', '50.ogg']) :-  D < 400, !.
dist(D, ['400.ogg']) :-  D < 450, !.
dist(D, ['400.ogg', '50.ogg']) :-  D < 500, !.
dist(D, ['500.ogg']) :-  D < 550, !.
dist(D, ['500.ogg', '50.ogg']) :-  D < 600, !.
dist(D, ['600.ogg']) :-  D < 650, !.
dist(D, ['600.ogg', '50.ogg']) :-  D < 700, !.
dist(D, ['700.ogg']) :-  D < 750, !.
dist(D, ['700.ogg', '50.ogg']) :-  D < 800, !.
dist(D, ['800.ogg']) :-  D < 850, !.
dist(D, ['800.ogg', '50.ogg']) :-  D < 900, !.
dist(D, ['900.ogg']) :-  D < 950, !.
dist(D, ['900.ogg', '50.ogg']) :-  !.


distance(Dist) == ['viac_ako.ogg', '1.ogg', 'kilometer.ogg'] :- Dist < 1500.
distance(Dist) == ['viac_ako.ogg', '2.ogg', 'kilometre.ogg'] :- Dist < 3000.
distance(Dist) == ['viac_ako.ogg', '3.ogg', 'kilometre.ogg'] :- Dist < 4000.
distance(Dist) == ['viac_ako.ogg', '4.ogg', 'kilometre.ogg'] :- Dist < 5000.
distance(Dist) == ['viac_ako.ogg', '5.ogg', 'kilometrov.ogg'] :- Dist < 6000.
distance(Dist) == ['viac_ako.ogg', '6.ogg', 'kilometrov.ogg'] :- Dist < 7000.
distance(Dist) == ['viac_ako.ogg', '7.ogg', 'kilometrov.ogg'] :- Dist < 8000.
distance(Dist) == ['viac_ako.ogg', '8.ogg', 'kilometrov.ogg'] :- Dist < 9000.
distance(Dist) == ['viac_ako.ogg', '9.ogg', 'kilometrov.ogg'] :- Dist < 10000.
distance(Dist) == ['viac_ako.ogg', X, 'kilometrov.ogg'] :- D is Dist/1000, dist(D, X).



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
