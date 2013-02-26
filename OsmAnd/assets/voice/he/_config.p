:- op('==', xfy, 500).
version(0).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['turn.ogg',   'left-e.ogg']).
turn('left_sh', ['sharp_left-e.ogg']).
turn('left_sl', ['turn.ogg', 'slight_left-e.ogg']).
turn('right', ['turn.ogg', 'right-e.ogg']).
turn('right_sh', ['sharp_right-e.ogg']).
turn('right_sl', ['turn.ogg', 'slight_right-e.ogg']).
turn('right_keep', ['keep_right-e.ogg']).
turn('left_keep', ['keep_left-e.ogg']).

prepare_turn(Turn, Dist) == ['Prepare_to-a.ogg', delay_450, D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['after.ogg', delay_250, D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Prepare_to-a.ogg', delay_300, D, delay_300,'turn_back-e.ogg'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['prepare_to-enter.ogg', 'after.ogg', D] :- 
		distance(Dist) == D.

make_ut(Dist) == ['after.ogg', delay_300, D, delay_300, 'turn_back-e.ogg'] :- 
			distance(Dist) == D.
make_ut == ['turn_back-e.ogg'].

make_ut_wp == ['when_possible_please_make_a_u_turn.ogg'].




roundabout(Dist, _Angle, Exit) == ['after.ogg', delay_300, D, delay_300, 'enter_the_roundabout-e.ogg', delay_250, 'and_take.ogg', 'exit-e.ogg', E] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['taking.ogg', delay_250, 'exit-e.ogg', E] :- nth(Exit, E).

and_arrive_destination == ['arrive_at_your_destination-e.ogg']. % Miss and?
reached_destination == ['you_have_reached_your_destination.ogg'].
and_arrive_intermediate == ['arrive_at_viapoint-e.ogg'].
reached_intermediate == ['you_have_reached_a_viapoint.ogg'].

then == ['then.ogg', delay_350].

bear_right == ['keep_right-e.ogg'].
bear_left == ['keep_left-e.ogg'].
route_recalc(Dist) == ['recalc.ogg', D]:- distance(Dist) == D.   %nothing to said possibly beep?	
route_new_calc(Dist) == ['the_trip_is_more_than.ogg', delay_150, D] :- distance(Dist) == D. % nothing to said possibly beep?

location_lost == ['gps_signal_lost.ogg'].

go_ahead(Dist) == ['Follow-the-road-for.ogg', delay_250,  D]:- distance(Dist) == D.
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
nth(10, '10th.ogg').
nth(11, '11th.ogg').
nth(12, '12th.ogg').
nth(13, '13th.ogg').
nth(14, '14th.ogg').
nth(15, '15th.ogg').
nth(16, '16th.ogg').
nth(17, '17th.ogg').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'meters-e.ogg',T).
dist(D, ['10.ogg']) :-  D < 20, !.
dist(D, ['20.ogg']) :-  D < 30, !.
dist(D, ['30.ogg']) :-  D < 40, !.
dist(D, ['40.ogg']) :-  D < 50, !.
dist(D, ['50.ogg']) :-  D < 60, !.
dist(D, ['60.ogg']) :-  D < 70, !.
dist(D, ['70.ogg']) :-  D < 80, !.
dist(D, ['80.ogg']) :-  D < 90, !.
dist(D, ['90.ogg']) :-  D < 100, !.
dist(D, ['100.ogg']) :-  D < 105, !.
dist(D, ['100.ogg', '10.ogg']) :-  D < 115, !.
dist(D, ['100.ogg', '20.ogg']) :-  D < 125, !.
dist(D, ['100.ogg', '30.ogg']) :-  D < 135, !.
dist(D, ['100.ogg', '40.ogg']) :-  D < 145, !.
dist(D, ['100.ogg', '50.ogg']) :-  D < 155, !.
dist(D, ['100.ogg', '60.ogg']) :-  D < 165, !.
dist(D, ['100.ogg', '70.ogg']) :-  D < 175, !.
dist(D, ['100.ogg', '80.ogg']) :-  D < 185, !.
dist(D, ['100.ogg', '90.ogg']) :-  D < 195, !.
dist(D, ['200.ogg']) :-  D < 205, !.
dist(D, ['200.ogg', '10.ogg']) :-  D < 215, !.
dist(D, ['200.ogg', '20.ogg']) :-  D < 225, !.
dist(D, ['200.ogg', '30.ogg']) :-  D < 235, !.
dist(D, ['200.ogg', '40.ogg']) :-  D < 245, !.
dist(D, ['200.ogg', '50.ogg']) :-  D < 255, !.
dist(D, ['200.ogg', '60.ogg']) :-  D < 265, !.
dist(D, ['200.ogg', '70.ogg']) :-  D < 275, !.
dist(D, ['200.ogg', '80.ogg']) :-  D < 285, !.
dist(D, ['200.ogg', '90.ogg']) :-  D < 295, !.
dist(D, ['300.ogg']) :-  D < 305, !.
dist(D, ['300.ogg', '10.ogg']) :-  D < 315, !.
dist(D, ['300.ogg', '20.ogg']) :-  D < 325, !.
dist(D, ['300.ogg', '30.ogg']) :-  D < 335, !.
dist(D, ['300.ogg', '40.ogg']) :-  D < 345, !.
dist(D, ['300.ogg', '50.ogg']) :-  D < 355, !.
dist(D, ['300.ogg', '60.ogg']) :-  D < 365, !.
dist(D, ['300.ogg', '70.ogg']) :-  D < 375, !.
dist(D, ['300.ogg', '80.ogg']) :-  D < 385, !.
dist(D, ['300.ogg', '90.ogg']) :-  D < 395, !.
dist(D, ['400.ogg']) :-  D < 405, !.
dist(D, ['400.ogg', '10.ogg']) :-  D < 415, !.
dist(D, ['400.ogg', '20.ogg']) :-  D < 425, !.
dist(D, ['400.ogg', '30.ogg']) :-  D < 435, !.
dist(D, ['400.ogg', '40.ogg']) :-  D < 445, !.
dist(D, ['400.ogg', '50.ogg']) :-  D < 455, !.
dist(D, ['400.ogg', '60.ogg']) :-  D < 465, !.
dist(D, ['400.ogg', '70.ogg']) :-  D < 475, !.
dist(D, ['400.ogg', '80.ogg']) :-  D < 485, !.
dist(D, ['400.ogg', '90.ogg']) :-  D < 495, !.
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


distance(Dist) == ['more_than.ogg', '1.ogg', 'kilometer-e.ogg'] :- Dist < 1500.
distance(Dist) == ['more_than.ogg', '2.ogg', 'kilometers-e.ogg'] :- Dist < 3000.
distance(Dist) == ['more_than.ogg', '3.ogg', 'kilometers-e.ogg'] :- Dist < 4000.
distance(Dist) == ['more_than.ogg', '4.ogg', 'kilometers-e.ogg'] :- Dist < 5000.
distance(Dist) == ['more_than.ogg', '5.ogg', 'kilometers-e.ogg'] :- Dist < 6000.
distance(Dist) == ['more_than.ogg', '6.ogg', 'kilometers-e.ogg'] :- Dist < 7000.
distance(Dist) == ['more_than.ogg', '7.ogg', 'kilometers-e.ogg'] :- Dist < 8000.
distance(Dist) == ['more_than.ogg', '8.ogg', 'kilometers-e.ogg'] :- Dist < 9000.
distance(Dist) == ['more_than.ogg', '9.ogg', 'kilometers-e.ogg'] :- Dist < 10000.
distance(Dist) == ['more_than.ogg', X, 'kilometers-e.ogg'] :- D is Dist/1000, dist(D, X).



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
