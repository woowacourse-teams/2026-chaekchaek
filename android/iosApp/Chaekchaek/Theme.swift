import SwiftUI

enum AppTheme {
    static let background = Color(red: 252 / 255, green: 250 / 255, blue: 247 / 255)
    static let surface = Color.white
    static let muted = Color(red: 247 / 255, green: 242 / 255, blue: 236 / 255)
    static let band = Color(red: 241 / 255, green: 233 / 255, blue: 222 / 255)
    static let ink = Color(red: 26 / 255, green: 26 / 255, blue: 26 / 255)
    static let secondary = Color(red: 102 / 255, green: 102 / 255, blue: 102 / 255)
    static let border = Color(red: 201 / 255, green: 201 / 255, blue: 201 / 255)
    static let accent = Color(red: 1, green: 152 / 255, blue: 0)
    static let accentSoft = Color(red: 1, green: 244 / 255, blue: 223 / 255)
}

struct BookCover: View {
    let book: Book

    var body: some View {
        AsyncImage(url: book.coverURL) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                AppTheme.band
                Image(systemName: "book.closed")
                    .foregroundStyle(AppTheme.secondary)
            }
        }
        .frame(width: 64, height: 92)
        .clipShape(RoundedRectangle(cornerRadius: 4))
        .accessibilityHidden(true)
    }
}
