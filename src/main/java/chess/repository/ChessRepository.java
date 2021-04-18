package chess.repository;

import chess.domain.game.ChessGame;
import chess.domain.piece.Color;
import chess.dto.ChessBoardDTO;
import chess.dto.FinishDTO;
import chess.dto.TurnDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ChessRepository {
    private JdbcTemplate jdbcTemplate;

    public ChessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findRoomId(String title) {
        String findTitleQuery = "SELECT id FROM chess_game WHERE BINARY title = ?";
        return jdbcTemplate.queryForList(findTitleQuery, String.class, title)
                .stream()
                .findAny();
    }

    public String addGame(ChessGame chessGame, String title) {
        String addingGameQuery = "INSERT INTO chess_game (turn, finished, board, title) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(addingGameQuery, chessGame.getTurn(), chessGame.isOver(), Serializer.serialize(chessGame)
                , title);
        String findingGameQuery = "SELECT MAX(id) FROM chess_game";
        return jdbcTemplate.queryForObject(findingGameQuery, String.class);
    }

    //DTO 를 컨트롤러에서 만든다.
    public ChessBoardDTO loadGameAsDTO(String gameId) {
        String loadingGameQuery = "SELECT board FROM chess_game WHERE id= ?";
        ChessBoardDTO chessBoardDTO = Serializer.deserializeAsDTO(jdbcTemplate.queryForObject(loadingGameQuery,
                String.class,
                gameId));
        return chessBoardDTO;
    }

    //체스게임을 리턴한다, 디티오 말고.
    public ChessGame loadGameById(String gameId) {
        String findingGameQuery = "SELECT board, turn FROM chess_game WHERE id= ?";
        return jdbcTemplate.queryForObject(findingGameQuery, (resultSet, rowNum) -> {
            return new ChessGame(
                    Serializer.deserialize(resultSet.getString("board")),
                    Color.of(resultSet.getString("turn")));
        }, gameId);
    }

    public ChessGame loadGameByName(String roomName) {
        String findingGameQuery = "SELECT board, turn FROM chess_game WHERE BINARY title = ?";
        return jdbcTemplate.queryForObject(findingGameQuery, (resultSet, rowNum) -> {
            return new ChessGame(
                    Serializer.deserialize(resultSet.getString("board")),
                    Color.of(resultSet.getString("turn")));
        }, roomName);
    }


    public TurnDTO turn(String gameId) {
        String findingTurnQuery = "SELECT turn FROM chess_game WHERE id = ?";
        TurnDTO turnDTO = new TurnDTO(jdbcTemplate.queryForObject(findingTurnQuery, String.class, gameId));
        return turnDTO;
    }

    public void saveGame(String gameId, ChessGame chessGame) {
        String savingGameQuery = "UPDATE chess_game SET turn = ?, board = ? WHERE id = ?";
        jdbcTemplate.update(savingGameQuery, chessGame.getTurn(), Serializer.serialize(chessGame), gameId);
    }

    public FinishDTO isFinishedById(String gameId) {
        String finishedQuery = "SELECT finished FROM chess_game WHERE id = ?";
        FinishDTO finishDTO = new FinishDTO(jdbcTemplate.queryForObject(finishedQuery, Boolean.class, gameId));
        return finishDTO;
    }

    public boolean isFinishedByName(String roomName) {
        String finishedQuery = "SELECT finished FROM chess_game WHERE BINARY title = ?";
        return jdbcTemplate.queryForObject(finishedQuery, Boolean.class, roomName);
    }

    public void finish(String gameId) {
        String savingGameQuery = "UPDATE chess_game SET finished = ? WHERE id = ?";
        jdbcTemplate.update(savingGameQuery, true, gameId);
    }

    public void restart(String gameId, ChessGame chessGame) {
        String restartQuery = "UPDATE chess_game SET turn = ?, finished = ?, board = ? WHERE id = ?";
        jdbcTemplate.update(restartQuery, chessGame.getTurn(), chessGame.isOver(), Serializer.serialize(chessGame),
                gameId);
    }

    public Map<String, String> findAllRooms() {
        String findAllQuery = "SELECT id, title FROM chess_game";

        return jdbcTemplate
                .queryForList(findAllQuery)
                .stream()
                .collect(Collectors.toMap(room -> room.get("id").toString(), room -> room.get("title").toString()));
    }
}
