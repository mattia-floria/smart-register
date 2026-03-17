class DocumentsResponse {
  List<DocumentRemoteModel> documents;
  List<SchoolReportRemoteModel> schoolReports;

  DocumentsResponse({this.documents, this.schoolReports});

  DocumentsResponse.fromJson(Map<String, dynamic> json) {
    if (json['documents'] != null) {
      documents = List<DocumentRemoteModel>();
      json['documents'].forEach((v) {
        documents.add(DocumentRemoteModel.fromJson(v));
      });
    }
    if (json['schoolReports'] != null) {
      schoolReports = List<SchoolReportRemoteModel>();
      json['schoolReports'].forEach((v) {
        schoolReports.add(SchoolReportRemoteModel.fromJson(v));
      });
    }
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = Map<String, dynamic>();
    data['documents'] = this.documents.map((v) => v.toJson()).toList();
      data['schoolReports'] =
        this.schoolReports.map((v) => v.toJson()).toList();
      return data;
  }
}

class DocumentRemoteModel {
  String hash;
  String desc;

  DocumentRemoteModel({this.hash, this.desc});

  DocumentRemoteModel.fromJson(Map<String, dynamic> json) {
    hash = json['hash'];
    desc = json['desc'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = Map<String, dynamic>();
    data['hash'] = this.hash;
    data['desc'] = this.desc;
    return data;
  }
}

class SchoolReportRemoteModel {
  String desc;
  String confirmLink;
  String viewLink;

  SchoolReportRemoteModel({this.desc, this.confirmLink, this.viewLink});

  SchoolReportRemoteModel.fromJson(Map<String, dynamic> json) {
    desc = json['desc'];
    confirmLink = json['confirmLink'];
    viewLink = json['viewLink'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = Map<String, dynamic>();
    data['desc'] = this.desc;
    data['confirmLink'] = this.confirmLink;
    data['viewLink'] = this.viewLink;
    return data;
  }
}
