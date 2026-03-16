import '../../../setup/domain/models/imported_audiobook.dart';
import '../../domain/models/audiobook.dart';
import 'audiobook_parser_service.dart';

class AudiobookMapperService {
  AudiobookMapperService(this._parser);

  final AudiobookParserService _parser;

  Future<List<Audiobook>> mapImported(List<ImportedAudiobook> imported) async {
    return Future.wait(
      imported.map(
        (item) => _parser.parseGroup(
          groupTitle: item.title,
          paths: item.filePaths,
          importedAt: item.importedAt,
        ),
      ),
    );
  }
}
