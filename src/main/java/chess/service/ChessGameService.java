package chess.service;

import chess.dao.ChessGameDAO;
import chess.dao.PieceDAO;
import chess.domain.board.Board;
import chess.domain.game.*;
import chess.domain.piece.Piece;
import chess.domain.piece.PieceFactory;
import chess.domain.piece.Position;
import chess.dto.ChessGameDto;
import chess.dto.ChessGameStatusDto;
import chess.dto.ScoreDto;
import chess.exception.AlreadyPlayingChessGameException;
import chess.exception.NoSuchPermittedChessPieceException;
import chess.exception.NotFoundPlayingChessGameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChessGameService {

    private final ChessGameDAO chessGameDAO;
    private final PieceDAO pieceDAO;

    public ChessGameService(ChessGameDAO chessGameDAO, PieceDAO pieceDAO) {
        this.chessGameDAO = chessGameDAO;
        this.pieceDAO = pieceDAO;
    }

    @Transactional
    public ChessGameDto createNewChessGame() {
        Optional<ChessGameEntity> latestChessGame = chessGameDAO.findByStateIsBlackTurnOrWhiteTurn();
        if (latestChessGame.isPresent()) {
            throw new AlreadyPlayingChessGameException();
        }

        Long chessGameId = chessGameDAO.create();
        List<Piece> pieces = PieceFactory.createPieces();
        pieceDAO.saveAll(chessGameId, pieces);
        ChessGame chessGame = new ChessGame(new Board(pieces));
        chessGame.changeState(new BlackTurn(chessGame));
        return new ChessGameDto(chessGame);
    }

    @Transactional
    public ChessGameDto moveChessPiece(final Position source, final Position target) {
        ChessGameEntity chessGameEntity = findStateIsBlackAndWhiteTurnGame();
        Long chessGameId = chessGameEntity.getId();
        ChessGame chessGame = findChessGameByChessGameId(chessGameEntity);
        Piece sourcePiece = pieceDAO.findOneByPosition(chessGameId, source.getRow(), source.getColumn())
                .orElseThrow(NoSuchPermittedChessPieceException::new);
        chessGame.move(sourcePiece.getPosition(), target);
        pieceDAO.findOneByPosition(chessGameId, target.getRow(), target.getColumn())
                .ifPresent(piece -> pieceDAO.delete(chessGameId, target.getRow(), target.getColumn()));

        sourcePiece.setPosition(target);
        pieceDAO.update(sourcePiece);
        chessGameDAO.updateState(chessGameId, chessGame.getState().getValue());

        return new ChessGameDto(chessGame);
    }

    @Transactional(readOnly = true)
    public ChessGameStatusDto findLatestChessGameStatus() {
        return chessGameDAO.findIsExistPlayingChessGameStatus();
    }

    @Transactional(readOnly = true)
    public ChessGameDto findLatestPlayingGame() {
        ChessGameEntity chessGameEntity = findStateIsBlackAndWhiteTurnGame();
        ChessGame chessGame = findChessGameByChessGameId(chessGameEntity);
        return new ChessGameDto(chessGame);
    }

    @Transactional
    public ChessGameDto endGame() {
        ChessGameEntity chessGameEntity = findStateIsBlackAndWhiteTurnGame();
        ChessGame chessGame = findChessGameByChessGameId(chessGameEntity);
        chessGame.end();
        chessGameDAO.updateState(chessGameEntity.getId(), chessGame.getState().getValue());

        return new ChessGameDto(chessGame);
    }

    @Transactional(readOnly = true)
    public ScoreDto calculateScores() {
        ChessGameEntity chessGameEntity = findStateIsBlackAndWhiteTurnGame();
        ChessGame chessGame = findChessGameByChessGameId(chessGameEntity);

        return new ScoreDto(chessGame);
    }

    private ChessGameEntity findStateIsBlackAndWhiteTurnGame() {
        return chessGameDAO.findByStateIsBlackTurnOrWhiteTurn()
                .orElseThrow(NotFoundPlayingChessGameException::new);
    }

    private ChessGame findChessGameByChessGameId(final ChessGameEntity chessGameEntity) {
        List<Piece> pieces = pieceDAO.findAllPiecesByChessGameId(chessGameEntity.getId());
        Board board = new Board(pieces);
        ChessGame chessGame = new ChessGame(board);
        State currentState = StateFactory.valueOf(chessGameEntity.getState(), chessGame);
        chessGame.changeState(currentState);

        return chessGame;
    }

}