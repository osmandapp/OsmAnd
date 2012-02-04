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

prepare_turn(Turn, Dist) == ['Prepare_to.ogg', 'after-m.ogg', delay_300, D, delay_300, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['after-m.ogg', delay_300, D, delay_300, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Prepare_to.ogg', 'after-m.ogg', delay_300, D, delay_300,'turn_back-e.ogg'] :- distance(Dist) == D.
make_ut(Dist) == ['after-m.ogg', delay_300, D, delay_300, 'turn_back-e.ogg'] :- distance(Dist) == D.
make_ut == ['turn_back-e.ogg'].

prepare_roundabout(Dist) == ['Prepare_to.ogg', 'after-m.ogg', delay_300, D, delay_300, 'cross_the_roundabout-e.ogg'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['after-m.ogg', delay_300, D, delay_300, 'enter_the_roundabout-e.ogg', delay_250, 'and_take_the.ogg', 
	delay_250, E, 'exit-e.ogg'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['taking_the.ogg', delay_250,  E, 'exit-e.ogg'] :- nth(Exit, E).

go_ahead == ['continue_straight-e.ogg'].
go_ahead(Dist) == ['Drive-n.ogg', delay_250,  D]:- distance(Dist) == D.

and_arrive_destination == ['arrive_at_your_destination-e.ogg'].

then == ['then.ogg', delay_350].
reached_destination == ['you_have_reached_your_destination.ogg'].
bear_right == ['keep_right-e.ogg'].
bear_left == ['keep_left-e.ogg'].

route_new_calc(Dist) == ['the_trip_is_more_than.ogg', delay_150, D] :- distance(Dist) == D.
route_recalc(Dist) == ['recalc.ogg' delay_150, D]:- distance(Dist) == D.

location_lost == ['gps_signal_lost.ogg'].


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
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'meters-10-e.ogg',T).
dist(D, ['10.ogg']) :-  D < 15, !.
dist(D, ['20.ogg']) :-  D < 25, !.
dist(D, ['30.ogg']) :-  D < 35, !.
dist(D, ['40.ogg']) :-  D < 45, !.
dist(D, ['50.ogg']) :-  D < 55, !.
dist(D, ['60.ogg']) :-  D < 65, !.
dist(D, ['70.ogg']) :-  D < 75, !.
dist(D, ['80.ogg']) :-  D < 85, !.
dist(D, ['90.ogg']) :-  D < 95, !.
dist(D, ['100.ogg']) :-  D < 1125, !.
dist(D, ['100_and.ogg', 'and_50.ogg']) :-  D < 175, !.
dist(D, ['200.ogg']) :-  D < 225, !.
dist(D, ['200_and.ogg', 'and_50.ogg']) :-  D < 275, !.
dist(D, ['300.ogg']) :-  D < 325, !.
dist(D, ['300_and.ogg', 'and_50.ogg']) :-  D < 375, !.
dist(D, ['400.ogg']) :-  D < 425, !.
dist(D, ['400_and.ogg', 'and_50.ogg']) :-  D < 475, !.
dist(D, ['500.ogg']) :-  D < 525, !.
dist(D, ['500_and.ogg', 'and_50.ogg']) :-  D < 675, !.
dist(D, ['600.ogg']) :-  D < 625, !.
dist(D, ['600_and.ogg', 'and_50.ogg']) :-  D < 675, !.
dist(D, ['700.ogg']) :-  D < 725, !.
dist(D, ['700_and.ogg', 'and_50.ogg']) :-  D < 775, !.
dist(D, ['800.ogg']) :-  D < 825, !.
dist(D, ['800_and.ogg', 'and_50.ogg']) :-  D < 875, !.
dist(D, ['900.ogg']) :-  D < 925, !.
dist(D, ['900_and.ogg', 'and_50.ogg']) :-  !.


distance(Dist) == ['more_than.ogg', '1.ogg', 'kilometer-e.ogg'] :- Dist < 1500.
distance(Dist) == ['more_than.ogg', '2.ogg', 'kilometers-1-2-e.ogg'] :- Dist < 2500.
distance(Dist) == ['more_than.ogg', '3.ogg', 'kilometers-1-2-e.ogg'] :- Dist < 3500.
distance(Dist) == ['more_than.ogg', '4.ogg', 'kilometers-1-2-e.ogg'] :- Dist < 4500.
distance(Dist) == ['more_than.ogg', '5.ogg', 'kilometers-200-e.ogg'] :- Dist < 5500.
distance(Dist) == ['more_than.ogg', '6.ogg', 'kilometers-200-e.ogg'] :- Dist < 6500.
distance(Dist) == ['more_than.ogg', '7.ogg', 'kilometers-200-e.ogg'] :- Dist < 7500.
distance(Dist) == ['more_than.ogg', '8.ogg', 'kilometers-200-e.ogg'] :- Dist < 8500.
distance(Dist) == ['more_than.ogg', '9.ogg', 'kilometers-200-e.ogg'] :- Dist < 9500.
distance(Dist) == ['more_than.ogg', X, 'kilometers-200-e.ogg'] :- D is Dist/1000, dist(D, X).


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