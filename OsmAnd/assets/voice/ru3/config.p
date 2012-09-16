:- op('==', xfy, 500).
version(0).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn_left-e.ogg']).
turn('left_sh', ['turn_sharp_left-e.ogg']).
turn('left_sl', ['turn_slightly_left-e.ogg']).
turn('right', ['turn_right-e.ogg']).
turn('right_sh', ['turn_sharp_right-e.ogg']).
turn('right_sl', ['turn_slightly_right-e.ogg']).
turn('right_keep', ['keep_right-e.ogg']).
turn('left_keep', ['keep_left-e.ogg']).
prepare_turn(Turn, Dist) == ['prepare_to.ogg', 'after-n.ogg', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['after-n.ogg', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['prepare_to.ogg', 'after-n.ogg', D, 'turn_back-e.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['prepare_to.ogg', 'after-n.ogg', D, 'cross_the_roundabout-e.ogg'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['after-n.ogg', D, 'turn_back-e.ogg'] :- 
			distance(Dist) == D.
make_ut == ['turn_back-e.ogg'].

roundabout(Dist, _Angle, Exit) == ['after-n.ogg',D, 'enter_the_roundabout-e.ogg', 'and_take_the.ogg', E, 'exit-e.ogg'] :- 
			distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['take_the.ogg', E, 'exit-e.ogg'] :- nth(Exit, E).

and_arrive_destination == ['then.ogg', delay_150, 'arrive_at_your_destination-e.ogg'].

reached_destination == ['you_have_reached_your_destination.ogg'].
and_arrive_intermediate == ['arrive_at_viapoint-e.ogg'].
reached_intermediate == ['you_have_reached_a_viapoint.ogg'].

then == ['then.ogg', delay_150].
bear_right == ['keep_right-e.ogg'].
location_lost == ['gps_signal_lost.ogg'].
bear_left == ['keep_left-e.ogg'].
% route_recalc(_Dist) == []. % 'recalc.ogg'
route_recalc(Dist) == Res :- go_ahead(Dist) == Res .
% route_new_calc(_Dist) == ['have_a_nice_trip_drive_carefully.ogg'].
route_new_calc(Dist) == Res :- go_ahead(Dist) == Res .

location_lost == ['gps_signal_lost.ogg']. 

go_ahead(Dist) == ['Drive-n.ogg', D]:- distance(Dist) == D.
go_ahead == ['continue_straight-e.ogg'].

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

%%% distance measure
distance(Dist) == [F, 'meters-e.ogg'] :- Dist < 1000, dist(Dist, F).
dist(D, '10.ogg') :-  D < 20, !.
dist(D, '20.ogg') :-  D < 30, !.
dist(D, '30.ogg') :-  D < 40, !.
dist(D, '40.ogg') :-  D < 50, !.
dist(D, '50.ogg') :-  D < 60, !.
dist(D, '60.ogg') :-  D < 70, !.
dist(D, '70.ogg') :-  D < 80, !.
dist(D, '80.ogg') :-  D < 90, !.
dist(D, '90.ogg') :-  D < 100, !.
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
dist(D, ['900.ogg', '50.ogg']) :-  D < 1000, !.



distance(Dist) == ['more_than.ogg', '1.ogg', 'kilometr.ogg'] :- Dist < 2000.
distance(Dist) == ['more_than.ogg', '2.ogg', 'kilometra.ogg'] :- Dist < 3000.
distance(Dist) == ['more_than.ogg', '3.ogg', 'kilometra.ogg'] :- Dist < 4000.
distance(Dist) == ['more_than.ogg', '4.ogg', 'kilometra.ogg'] :- Dist < 5000.
distance(Dist) == ['more_than.ogg', '5.ogg', 'kilometrov.ogg'] :- Dist < 6000.
distance(Dist) == ['more_than.ogg', '6.ogg', 'kilometrov.ogg'] :- Dist < 7000.
distance(Dist) == ['more_than.ogg', '7.ogg', 'kilometrov.ogg'] :- Dist < 8000.
distance(Dist) == ['more_than.ogg', '8.ogg', 'kilometrov.ogg'] :- Dist < 9000.
distance(Dist) == ['more_than.ogg', '9.ogg', 'kilometrov.ogg'] :- Dist < 10000.
distance(Dist) == ['more_than.ogg', X, 'kilometrov.ogg'] :- D is Dist/1000, dist(D, X).



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
