bool needUpdate(int lastUpdate) {
  return DateTime.fromMillisecondsSinceEpoch(lastUpdate)
          .isBefore(DateTime.now().subtract(Duration(minutes: 2)));
}
